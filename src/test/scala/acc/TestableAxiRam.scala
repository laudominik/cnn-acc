import chisel3._
import chisel3.util._

class TestableAxiRamIO(
  dataWidth: Int,
  addrWidth: Int,
  idWidth: Int
) extends Bundle {
  val axi = Flipped(new AxiMasterIO(dataWidth, addrWidth, idWidth))
  val testWrCmd = Flipped(Decoupled(new AxiRamWrCtrlCmd(dataWidth, addrWidth)))
  val testRdCmd = Flipped(Decoupled(new AxiRamRdCtrlCmd(dataWidth, addrWidth)))
  val testRdResp = Decoupled(new AxiRamRdResp(dataWidth, addrWidth))
}

class TestableAxiRam(
  dataWidth: Int,
  addrWidth: Int,
  idWidth: Int = 8,
  idReal: Int = 0,
  idTest: Int = 1
) extends Module {
  val io = IO(new TestableAxiRamIO(dataWidth, addrWidth, idWidth))
  val ram = Module(new AxiRam(dataWidth, addrWidth, idWidth))
  
  val crossbar = Module(new AxiCrossbarWrap2x1(dataWidth, addrWidth, idWidth))
  val testCtrl = Module(new AxiRamCtrl(dataWidth, addrWidth, idWidth))

  crossbar.io.clk := clock
  crossbar.io.rst := reset
  ram.io.clk := clock
  ram.io.rst := reset

  // test input
  testCtrl.io.wrCmd <> io.testWrCmd 
  testCtrl.io.rdCmd <> io.testRdCmd
  io.testRdResp <> testCtrl.io.rdResp

  // RAM
  ram.io.s_axi_awid     := crossbar.io.m00_axi_awid
  ram.io.s_axi_awaddr   := crossbar.io.m00_axi_awaddr
  ram.io.s_axi_awlen    := crossbar.io.m00_axi_awlen
  ram.io.s_axi_awsize   := crossbar.io.m00_axi_awsize
  ram.io.s_axi_awburst  := crossbar.io.m00_axi_awburst
  ram.io.s_axi_awlock   := crossbar.io.m00_axi_awlock
  ram.io.s_axi_awcache  := crossbar.io.m00_axi_awcache
  ram.io.s_axi_awprot   := crossbar.io.m00_axi_awprot
  ram.io.s_axi_awvalid  := crossbar.io.m00_axi_awvalid
  crossbar.io.m00_axi_awready  := ram.io.s_axi_awready

  ram.io.s_axi_wdata    := crossbar.io.m00_axi_wdata 
  ram.io.s_axi_wstrb    := crossbar.io.m00_axi_wstrb
  ram.io.s_axi_wlast    := crossbar.io.m00_axi_wlast
  ram.io.s_axi_wvalid   := crossbar.io.m00_axi_wvalid
  crossbar.io.m00_axi_wready   := ram.io.s_axi_wready

  crossbar.io.m00_axi_bid      := ram.io.s_axi_bid 
  crossbar.io.m00_axi_bresp    := ram.io.s_axi_bresp 
  crossbar.io.m00_axi_bvalid   := ram.io.s_axi_bvalid 
  ram.io.s_axi_bready         := crossbar.io.m00_axi_bready

  ram.io.s_axi_arid           := crossbar.io.m00_axi_arid
  ram.io.s_axi_araddr         := crossbar.io.m00_axi_araddr
  ram.io.s_axi_arlen          := crossbar.io.m00_axi_arlen
  ram.io.s_axi_arsize         := crossbar.io.m00_axi_arsize
  ram.io.s_axi_arburst        := crossbar.io.m00_axi_arburst
  ram.io.s_axi_arlock         := crossbar.io.m00_axi_arlock
  ram.io.s_axi_arcache        := crossbar.io.m00_axi_arcache
  ram.io.s_axi_arprot         := crossbar.io.m00_axi_arprot
  ram.io.s_axi_arvalid        := crossbar.io.m00_axi_arvalid
  crossbar.io.m00_axi_arready := ram.io.s_axi_arready 

  crossbar.io.m00_axi_rid     := ram.io.s_axi_rid 
  crossbar.io.m00_axi_rdata   := ram.io.s_axi_rdata
  crossbar.io.m00_axi_rresp   := ram.io.s_axi_rresp
  crossbar.io.m00_axi_rlast   := ram.io.s_axi_rlast
  crossbar.io.m00_axi_rvalid  := ram.io.s_axi_rvalid
  ram.io.s_axi_rready         := crossbar.io.m00_axi_rready

  // REAL AXI
  crossbar.io.s00_axi_awid    := io.axi.aw.id
  crossbar.io.s00_axi_awaddr  := io.axi.aw.addr
  crossbar.io.s00_axi_awlen   := io.axi.aw.len
  crossbar.io.s00_axi_awsize  := io.axi.aw.size
  crossbar.io.s00_axi_awburst := io.axi.aw.burst
  crossbar.io.s00_axi_awlock  := io.axi.aw.lock
  crossbar.io.s00_axi_awcache := io.axi.aw.cache
  crossbar.io.s00_axi_awprot  := io.axi.aw.prot
  crossbar.io.s00_axi_awvalid := io.axi.aw.valid
  io.axi.aw.ready             := crossbar.io.s00_axi_awready

  crossbar.io.s00_axi_wdata   := io.axi.w.data
  crossbar.io.s00_axi_wstrb   := io.axi.w.strb
  crossbar.io.s00_axi_wlast   := io.axi.w.last
  crossbar.io.s00_axi_wvalid  := io.axi.w.valid
  io.axi.w.ready              := crossbar.io.s00_axi_wready

  io.axi.b.id                 := crossbar.io.s00_axi_bid
  io.axi.b.resp               := crossbar.io.s00_axi_bresp
  io.axi.b.valid              := crossbar.io.s00_axi_bvalid
  crossbar.io.s00_axi_bready  := io.axi.b.ready

  crossbar.io.s00_axi_arid    := io.axi.ar.id
  crossbar.io.s00_axi_araddr  := io.axi.ar.addr
  crossbar.io.s00_axi_arlen   := io.axi.ar.len
  crossbar.io.s00_axi_arsize  := io.axi.ar.size
  crossbar.io.s00_axi_arburst := io.axi.ar.burst
  crossbar.io.s00_axi_arlock  := io.axi.ar.lock
  crossbar.io.s00_axi_arcache := io.axi.ar.cache
  crossbar.io.s00_axi_arprot  := io.axi.ar.prot
  crossbar.io.s00_axi_arvalid := io.axi.ar.valid
  io.axi.ar.ready             := crossbar.io.s00_axi_arready

  io.axi.r.id                 := crossbar.io.s00_axi_rid
  io.axi.r.data               := crossbar.io.s00_axi_rdata
  io.axi.r.resp               := crossbar.io.s00_axi_rresp
  io.axi.r.last               := crossbar.io.s00_axi_rlast
  io.axi.r.valid              := crossbar.io.s00_axi_rvalid
  crossbar.io.s00_axi_rready  := io.axi.r.ready

  // TEST AXI
  crossbar.io.s01_axi_awid    := testCtrl.io.axi.aw.id
  crossbar.io.s01_axi_awaddr  := testCtrl.io.axi.aw.addr
  crossbar.io.s01_axi_awlen   := testCtrl.io.axi.aw.len
  crossbar.io.s01_axi_awsize  := testCtrl.io.axi.aw.size
  crossbar.io.s01_axi_awburst := testCtrl.io.axi.aw.burst
  crossbar.io.s01_axi_awlock  := testCtrl.io.axi.aw.lock
  crossbar.io.s01_axi_awcache := testCtrl.io.axi.aw.cache
  crossbar.io.s01_axi_awprot  := testCtrl.io.axi.aw.prot
  crossbar.io.s01_axi_awvalid := testCtrl.io.axi.aw.valid
  testCtrl.io.axi.aw.ready     := crossbar.io.s01_axi_awready

  crossbar.io.s01_axi_wdata   := testCtrl.io.axi.w.data
  crossbar.io.s01_axi_wstrb   := testCtrl.io.axi.w.strb
  crossbar.io.s01_axi_wlast   := testCtrl.io.axi.w.last
  crossbar.io.s01_axi_wvalid  := testCtrl.io.axi.w.valid
  testCtrl.io.axi.w.ready     := crossbar.io.s01_axi_wready

  testCtrl.io.axi.b.id        := crossbar.io.s01_axi_bid
  testCtrl.io.axi.b.resp      := crossbar.io.s01_axi_bresp
  testCtrl.io.axi.b.valid     := crossbar.io.s01_axi_bvalid
  crossbar.io.s01_axi_bready  := testCtrl.io.axi.b.ready

  crossbar.io.s01_axi_arid    := testCtrl.io.axi.ar.id
  crossbar.io.s01_axi_araddr  := testCtrl.io.axi.ar.addr
  crossbar.io.s01_axi_arlen   := testCtrl.io.axi.ar.len
  crossbar.io.s01_axi_arsize  := testCtrl.io.axi.ar.size
  crossbar.io.s01_axi_arburst := testCtrl.io.axi.ar.burst
  crossbar.io.s01_axi_arlock  := testCtrl.io.axi.ar.lock
  crossbar.io.s01_axi_arcache := testCtrl.io.axi.ar.cache
  crossbar.io.s01_axi_arprot  := testCtrl.io.axi.ar.prot
  crossbar.io.s01_axi_arvalid := testCtrl.io.axi.ar.valid
  testCtrl.io.axi.ar.ready    := crossbar.io.s01_axi_arready

  testCtrl.io.axi.r.id        := crossbar.io.s01_axi_rid
  testCtrl.io.axi.r.data      := crossbar.io.s01_axi_rdata
  testCtrl.io.axi.r.resp      := crossbar.io.s01_axi_rresp
  testCtrl.io.axi.r.last      := crossbar.io.s01_axi_rlast
  testCtrl.io.axi.r.valid     := crossbar.io.s01_axi_rvalid
  crossbar.io.s01_axi_rready  := testCtrl.io.axi.r.ready

  // optional bands
  crossbar.io.s00_axi_awqos := 0.U
  crossbar.io.s00_axi_awuser := 0.U
  crossbar.io.s00_axi_wuser  := 0.U
  crossbar.io.s00_axi_arqos := 0.U
  crossbar.io.s00_axi_aruser := 0.U
  crossbar.io.s01_axi_awqos := 0.U
  crossbar.io.s01_axi_awuser := 0.U
  crossbar.io.s01_axi_wuser  := 0.U
  crossbar.io.s01_axi_arqos := 0.U
  crossbar.io.s01_axi_aruser := 0.U
  crossbar.io.m00_axi_buser  := 0.U
  crossbar.io.m00_axi_ruser  := 0.U

}