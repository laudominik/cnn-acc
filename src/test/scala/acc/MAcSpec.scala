import chisel3._
import chisel3.simulator.scalatest.ChiselSim
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import scala.util.Random

class MAcSpec extends AnyFreeSpec with Matchers with ChiselSim {
  val latency = 1

  "MAc should produce correct operation results" in {
    simulate(new MAc(8)) { dut =>
      val rnd = new Random()

      for (_ <- 0 until 10) {
        val input = rnd.nextInt(256) 
        val weight = rnd.nextInt(256)
        val accumulator = rnd.nextInt(512)

        dut.io.input.poke(input.U)
        dut.io.weight.poke(weight.U)
        dut.io.accumulator.poke(accumulator.U)
        dut.clock.step(latency)

        val expected = input * weight + accumulator
        dut.io.out.expect(expected.U)
      }
  
    }
  }
}