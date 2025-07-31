import chisel3._
import chisel3.simulator.scalatest.ChiselSim
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers

class BankedBufferSpec extends AnyFreeSpec with Matchers with ChiselSim {
  "BankedBuffer should correctly write and read multiple banks" in {
    simulate(new BankedBuffer(numBanks = 4, bankDepth = 8, width = 16)) { dut =>      
      for (i <- 0 until 4) {
        dut.io.wrEn(i).poke(false.B)
        dut.io.wrAddr(i).poke(0.U)
        dut.io.wrData(i).poke(0.U)
        dut.io.rdEn(i).poke(false.B)
        dut.io.rdAddr(i).poke(0.U)
      }
      dut.clock.step()

      for (addr <- 0 until 8) {
        for (bank <- 0 until 4) {
          dut.io.wrEn(bank).poke(true.B)
          dut.io.wrAddr(bank).poke(addr.U)
          dut.io.wrData(bank).poke((bank * 1000 + addr).U)
        }
        dut.clock.step()
      }

      for (bank <- 0 until 4) {
        dut.io.wrEn(bank).poke(false.B)
      }
      dut.clock.step()

      for (addr <- 0 until 8) {
        for (bank <- 0 until 4) {
          dut.io.rdEn(bank).poke(true.B)
          dut.io.rdAddr(bank).poke(addr.U)
        }
        dut.clock.step()

        for (bank <- 0 until 4) {
          val expected = bank * 1000 + addr
          dut.io.rdData(bank).expect(expected.U)
        }
      }

      for (bank <- 0 until 4) {
        dut.io.rdEn(bank).poke(false.B)
      }
    }
  }
}