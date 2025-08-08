import chisel3._
import chisel3.simulator.scalatest.ChiselSim
import chisel3.util._
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import scala.util.Random

class AxiRamCtrlSpec extends AnyFreeSpec with Matchers with ChiselSim {
  val latency = 1
  val dataWidth = 32
  val addrWidth = 16
  val idWidth = 8
  
  "AxiRamCtrl should carry out sent commands" in {
    simulate(
      new Module {
        val io = IO(new Bundle {
          val wrCmd = Flipped(Decoupled(new AxiRamWrCtrlCmd(dataWidth, addrWidth)))
          val rdCmd = Flipped(Decoupled(new AxiRamRdCtrlCmd(dataWidth, addrWidth)))
          val rdResp = Decoupled(new AxiRamRdResp(dataWidth, addrWidth))
        })

        val ctrl = Module(new AxiRamCtrl(dataWidth, addrWidth, idWidth))
        val ram = Module(new AxiRam(dataWidth, addrWidth, idWidth))
        
        ctrl.io.wrCmd <> io.wrCmd
        ctrl.io.rdCmd <> io.rdCmd
        ctrl.io.rdResp <> io.rdResp
        
        ram.io.clk := clock
        ram.io.rst := reset

        ram.io.s_axi_awid     := ctrl.io.axi.aw.id
        ram.io.s_axi_awaddr   := ctrl.io.axi.aw.addr
        ram.io.s_axi_awlen    := ctrl.io.axi.aw.len
        ram.io.s_axi_awsize   := ctrl.io.axi.aw.size
        ram.io.s_axi_awburst  := ctrl.io.axi.aw.burst
        ram.io.s_axi_awlock   := ctrl.io.axi.aw.lock
        ram.io.s_axi_awcache  := ctrl.io.axi.aw.cache
        ram.io.s_axi_awprot   := ctrl.io.axi.aw.prot
        ram.io.s_axi_awvalid  := ctrl.io.axi.aw.valid
        ctrl.io.axi.aw.ready  := ram.io.s_axi_awready

        ram.io.s_axi_wdata    := ctrl.io.axi.w.data 
        ram.io.s_axi_wstrb    := ctrl.io.axi.w.strb
        ram.io.s_axi_wlast    := ctrl.io.axi.w.last
        ram.io.s_axi_wvalid   := ctrl.io.axi.w.valid
        ctrl.io.axi.w.ready   := ram.io.s_axi_wready

        ctrl.io.axi.b.id      := ram.io.s_axi_bid 
        ctrl.io.axi.b.resp    := ram.io.s_axi_bresp 
        ctrl.io.axi.b.valid   := ram.io.s_axi_bvalid 
        ram.io.s_axi_bready   := ctrl.io.axi.b.ready

        ram.io.s_axi_arid     := ctrl.io.axi.ar.id
        ram.io.s_axi_araddr   := ctrl.io.axi.ar.addr
        ram.io.s_axi_arlen    := ctrl.io.axi.ar.len
        ram.io.s_axi_arsize   := ctrl.io.axi.ar.size
        ram.io.s_axi_arburst  := ctrl.io.axi.ar.burst
        ram.io.s_axi_arlock   := ctrl.io.axi.ar.lock
        ram.io.s_axi_arcache  := ctrl.io.axi.ar.cache
        ram.io.s_axi_arprot   := ctrl.io.axi.ar.prot
        ram.io.s_axi_arvalid  := ctrl.io.axi.ar.valid
        ctrl.io.axi.ar.ready  := ram.io.s_axi_arready 

        ctrl.io.axi.r.id      := ram.io.s_axi_rid 
        ctrl.io.axi.r.data    := ram.io.s_axi_rdata
        ctrl.io.axi.r.resp    := ram.io.s_axi_rresp
        ctrl.io.axi.r.last    := ram.io.s_axi_rlast
        ctrl.io.axi.r.valid   := ram.io.s_axi_rvalid
        ram.io.s_axi_rready   := ctrl.io.axi.r.ready
      }) { dut =>
        dut.reset.poke(true)
        dut.clock.step()
        dut.reset.poke(false)
        dut.clock.step()

        def sendWriteCmd(
          addr: Int,
          data: Int,
          wstrb: Int = 0xF
        ): Unit = {
          while (!dut.io.wrCmd.ready.peek().litToBoolean) {
            dut.clock.step()
          }

          dut.io.wrCmd.bits.addr.poke(addr.U)
          dut.io.wrCmd.bits.len.poke(0.U)
          dut.io.wrCmd.bits.data.poke(data.U)
          dut.io.wrCmd.bits.wstrb.poke(wstrb.U)
          dut.io.wrCmd.valid.poke(true.B)
          dut.clock.step()
          dut.io.wrCmd.valid.poke(false.B)
          while (!dut.io.wrCmd.ready.peek().litToBoolean) {
            dut.clock.step()
          }
        }

        def readData(
          addr: Int,
          len: Int
        ): Seq[BigInt] = {
          dut.io.rdCmd.bits.addr.poke(addr.U)
          dut.io.rdCmd.bits.len.poke((len - 1).U)
          dut.io.rdCmd.valid.poke(true)

          while (!dut.io.rdCmd.ready.peek().litToBoolean) {
            dut.clock.step()
          }
          dut.clock.step()
          dut.io.rdCmd.valid.poke(false)

          val data = scala.collection.mutable.ArrayBuffer[BigInt]()
          var received = 0
          while (received < len) {
            if (dut.io.rdResp.valid.peek().litToBoolean) {
              data += dut.io.rdResp.bits.data.peek().litValue
              received += 1
            }
            dut.io.rdResp.ready.poke(true)
            dut.clock.step()
          }
          dut.io.rdResp.ready.poke(false)
          data.toSeq
        }

        sendWriteCmd(0x0, 0x12)
        sendWriteCmd(0x1, 0xcc00, 0b0010)
        sendWriteCmd(0x4, 0x0ccccccc, 0b1111)
        assert(readData(0x0, 1).head == 0x0000cc12)
        assert(readData(0x1, 1).head == 0x0000cc12)
        val rd2 = readData(0x0, 2)
        assert(rd2.length == 2)
        assert(rd2(0) == 0x0000cc12)
        assert(rd2(1) == 0x0ccccccc)
        assert(readData(0x4, 1).head == 0x0ccccccc)
        sendWriteCmd(0x4, 0x00dd0000, 0b0100)
        assert(readData(0x4, 1).head == 0x0cddcccc)
        sendWriteCmd(0x8, 0x0cddcc00, 0b1111)
        sendWriteCmd(0x8, 0x000000ee, 0b0001)
        assert(readData(0x8, 1).head == 0x0cddccee)
        sendWriteCmd(0x8, 0x0000aa00, 0b0010)
        assert(readData(0x8, 1).head == 0x0cddaaee)
        sendWriteCmd(0x8, 0x00bb0000, 0b0100)
        assert(readData(0x8, 1).head == 0x0cbbaaee)
        sendWriteCmd(0x8, 0x77000000, 0b1000)
        assert(readData(0x8, 1).head == 0x77bbaaee)
        sendWriteCmd(0x8, 0x12345678)
        assert(readData(0x8, 1).head == 0x12345678)
        sendWriteCmd(0x8, 0x00ffee00, 0b0110)
        assert(readData(0x8, 1).head == 0x12ffee78)
        sendWriteCmd(0x8, 0x7a0000bb, 0b1001)
        assert(readData(0x8, 1).head == 0x7affeebb)
        sendWriteCmd(0xC, 0x7eadbeef)
        val burst = readData(0x8, 2)
        assert(burst.length == 2)
        assert(burst(0) == 0x7affeebb)
        assert(burst(1) == 0x7eadbeef)
        sendWriteCmd(0xF, 0x78000000, 0b1000)
        val rdOverlap = readData(0xC, 1)
        assert(rdOverlap.head == 0x78adbEEF)
        sendWriteCmd(0x10, 0x00000000)
        assert(readData(0x10, 1).head == 0x00000000)
        val before = readData(0x10, 1).head
        sendWriteCmd(0x10, 0x7fffffff, 0x0)
        val after = readData(0x10, 1).head
        assert(before == after)
        sendWriteCmd(0x14, 0x0000beef)
        sendWriteCmd(0x14, 0x00ff0000, 0b0100)
        assert(readData(0x14, 1).head == 0x00ffbeef)
    }
  }
}