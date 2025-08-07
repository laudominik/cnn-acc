import chisel3._
import chisel3.experimental.BundleLiterals._
import chisel3.simulator.scalatest.ChiselSim
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers

class ProcessingArraySpec extends AnyFreeSpec with Matchers with ChiselSim {

  "ProcessingArray should correctly accumulate sums along the columns" in {
    val rows = 4
    val cols = 5
    val dataWidth = 8
    val latency = 2 * cols

    simulate(new ProcessingArray(rows, cols, dataWidth)) { dut =>
      dut.reset.poke(true.B)
      dut.clock.step()
      dut.reset.poke(false.B)
      dut.clock.step()

      val rand = new scala.util.Random
      val inputs = Seq.fill(cols)(rand.nextInt(256).U(dataWidth.W))
      val weights = Seq.fill(rows)(rand.nextInt(256).U(dataWidth.W))

      for (r <- 0 until rows) {
        dut.io.weights(r).poke(weights(r))
      }
      for (c <- 0 until cols) {
        dut.io.inputs(c).poke(inputs(c))
      }

      val expected = (0 until rows).map { r =>
        ((0 until cols).map { c=>
          inputs(c).litValue * weights(r).litValue
        }).sum
      }

      dut.clock.step(latency)

      for (r <- 0 until rows) {
        dut.io.out(r).expect(expected(r).U(2 * dataWidth - 1, 0))
      }
    }
  }
}