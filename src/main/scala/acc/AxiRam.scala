import chisel3._
import chisel3.util._

class AxiRam(
  val DATA_WIDTH: Int = 32,
  val ADDR_WIDTH: Int = 16,
  val ID_WIDTH: Int = 8,
  val PIPELINE_OUTPUT: Int = 0
) extends BlackBox(Map(
  "DATA_WIDTH" -> DATA_WIDTH,
  "ADDR_WIDTH" -> ADDR_WIDTH,
  "STRB_WIDTH" -> (DATA_WIDTH / 8),
  "ID_WIDTH" -> ID_WIDTH,
  "PIPELINE_OUTPUT" -> PIPELINE_OUTPUT
)) with HasBlackBoxResource {

  val io = IO(new Bundle {
    val clk  = Input(Clock())
    val rst  = Input(Bool())

    val s_axi_awid     = Input(UInt(ID_WIDTH.W))
    val s_axi_awaddr   = Input(UInt(ADDR_WIDTH.W))
    val s_axi_awlen    = Input(UInt(8.W))
    val s_axi_awsize   = Input(UInt(3.W))
    val s_axi_awburst  = Input(UInt(2.W))
    val s_axi_awlock   = Input(Bool())
    val s_axi_awcache  = Input(UInt(4.W))
    val s_axi_awprot   = Input(UInt(3.W))
    val s_axi_awvalid  = Input(Bool())
    val s_axi_awready  = Output(Bool())

    val s_axi_wdata    = Input(UInt(DATA_WIDTH.W))
    val s_axi_wstrb    = Input(UInt((DATA_WIDTH / 8).W))
    val s_axi_wlast    = Input(Bool())
    val s_axi_wvalid   = Input(Bool())
    val s_axi_wready   = Output(Bool())

    val s_axi_bid      = Output(UInt(ID_WIDTH.W))
    val s_axi_bresp    = Output(UInt(2.W))
    val s_axi_bvalid   = Output(Bool())
    val s_axi_bready   = Input(Bool())

    val s_axi_arid     = Input(UInt(ID_WIDTH.W))
    val s_axi_araddr   = Input(UInt(ADDR_WIDTH.W))
    val s_axi_arlen    = Input(UInt(8.W))
    val s_axi_arsize   = Input(UInt(3.W))
    val s_axi_arburst  = Input(UInt(2.W))
    val s_axi_arlock   = Input(Bool())
    val s_axi_arcache  = Input(UInt(4.W))
    val s_axi_arprot   = Input(UInt(3.W))
    val s_axi_arvalid  = Input(Bool())
    val s_axi_arready  = Output(Bool())

    val s_axi_rid      = Output(UInt(ID_WIDTH.W))
    val s_axi_rdata    = Output(UInt(DATA_WIDTH.W))
    val s_axi_rresp    = Output(UInt(2.W))
    val s_axi_rlast    = Output(Bool())
    val s_axi_rvalid   = Output(Bool())
    val s_axi_rready   = Input(Bool())
  })

  addResource("/axi_ram.v")
}
