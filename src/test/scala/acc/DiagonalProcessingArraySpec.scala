import chisel3._
import chisel3.experimental.BundleLiterals._
import chisel3.simulator.scalatest.ChiselSim
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers

class DiagonalProcessingArraySpec extends AnyFreeSpec with Matchers with ChiselSim {
  "DiagonalProcessingArray should correctly accumulate sums along diagonals" in {
    val weights = 4
    val inputs = 5
    val dataWidth = 8
    val cols = weights
    val rows = inputs + 1 - cols
    val latency = cols * 2 + 2 

    simulate(new DiagonalProcessingArray(weights, inputs, dataWidth)) { dut =>
      dut.reset.poke(true.B)
      dut.clock.step()
      dut.reset.poke(false.B)
      dut.clock.step()

      val rand = new scala.util.Random
      val inputVals = Seq.fill(inputs)(rand.nextInt(256))
      val weightVals = Seq.fill(weights)(rand.nextInt(256))

      for (i <- 0 until inputs) {
        dut.io.inputs(i).poke(inputVals(i).U)
      }
      for (w <- 0 until weights) {
        dut.io.weights(w).poke(weightVals(w).U)
      }

      val expected = (0 until rows).map { r =>
        (0 until cols).map { c =>
          val inputIdx = r + c
          if (inputIdx < inputVals.length)
            inputVals(inputIdx) * weightVals(c)
          else 0
        }.sum
      }

      dut.clock.step(latency)

      for (r <- 0 until rows) {
        dut.io.out(r).expect(expected(r).U(2 * dataWidth - 1, 0))
      }
    }
  }
}