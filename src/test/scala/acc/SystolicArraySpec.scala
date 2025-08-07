import chisel3._
import chisel3.experimental.BundleLiterals._
import chisel3.simulator.scalatest.ChiselSim
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers

class SystolicArraySpec extends AnyFreeSpec with Matchers with ChiselSim {
  "SystolicArraySpec should pipeline inputs and produce correct outputs" in {
    
    val cols = 3
    val rows = 3
    val dataWidth = 8
    val firstResultLatency = rows + 1 // time after the first result appears (which is c(0,0))
    val totalLatency = 2 * rows + cols

    simulate(new SystolicArray(rows, cols, dataWidth)) { dut =>
      dut.reset.poke(true.B)
      dut.clock.step()
      dut.reset.poke(false.B)
      dut.clock.step()

      val weights = Seq(
            Seq(9, 8, 7),
            Seq(6, 5, 4),
            Seq(3, 2, 1)
      )
      val inputs = Seq(
        Seq(1, 2, 3),
        Seq(4, 5, 6),
        Seq(7, 8, 9)
      )
      val expected = Seq(
        Seq(54, 0, 0),      // t = 4
        Seq(72, 42, 0),     // t = 5
        Seq(90, 57, 30),    // t = 6
        Seq(0, 72, 42),     // t = 8
        Seq(0, 0, 54)       // t = 9
      )

      for (i <- 0 until rows; j <- 0 until cols) {
        dut.io.weights(i)(j).poke(weights(i)(j).U)
      }



      // feed the data:
      // feed queue for row=0: [0  0  3  2  1] -> input
      // feed queue for row=1: [0  6  5  4  0] -> input
      // feed queue for row=2: [9  8  7  0  0] -> input
      //
      // meaning each row has "row"-cycles offset
      def getInputAtTime(t: Int, r: Int): Int = {
        val j = t - r
        if (j >= 0 && j < cols) inputs(r)(j) else 0
      }

      for (t <- 0 until totalLatency) {
        for (r <- 0 until rows) {
          val value = getInputAtTime(t, r)
          dut.io.inputs(r).poke(value.U)
        }

        if (t < firstResultLatency) {
          dut.clock.step()
        } else {
          val expectedVector = expected(t - firstResultLatency)

          for (c <- 0 until cols) {
            dut.io.out(c).expect(expectedVector(c))
          }
          dut.clock.step()
        }
      }

    

    }
  }
}
