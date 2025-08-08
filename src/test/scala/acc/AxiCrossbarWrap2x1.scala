import chisel3._
import chisel3.util._

class AxiCrossbarWrap2x1(
  val DATA_WIDTH: Int = 32,
  val ADDR_WIDTH: Int = 32,
  val S_ID_WIDTH: Int = 8,
  val S_COUNT: Int = 2,
  val AWUSER_ENABLE: Int = 0,
  val AWUSER_WIDTH: Int = 1,
  val WUSER_ENABLE: Int = 0,
  val WUSER_WIDTH: Int = 1,
  val BUSER_ENABLE: Int = 0,
  val BUSER_WIDTH: Int = 1,
  val ARUSER_ENABLE: Int = 0,
  val ARUSER_WIDTH: Int = 1,
  val RUSER_ENABLE: Int = 0,
  val RUSER_WIDTH: Int = 1,
  val S00_THREADS: Int = 2,
  val S00_ACCEPT: Int = 16,
  val S01_THREADS: Int = 2,
  val S01_ACCEPT: Int = 16,
  val M_REGIONS: Int = 1,
  val M00_BASE_ADDR: BigInt = 0,
  val M00_ADDR_WIDTH: BigInt = BigInt(0xFFFFFF),
  val M00_CONNECT_READ: Int = 0x3,
  val M00_CONNECT_WRITE: Int = 0x3,
  val M00_ISSUE: Int = 4,
  val M00_SECURE: Int = 0,
  val S00_AW_REG_TYPE: Int = 0,
  val S00_W_REG_TYPE: Int = 0,
  val S00_B_REG_TYPE: Int = 1,
  val S00_AR_REG_TYPE: Int = 0,
  val S00_R_REG_TYPE: Int = 2,
  val S01_AW_REG_TYPE: Int = 0,
  val S01_W_REG_TYPE: Int = 0,
  val S01_B_REG_TYPE: Int = 1,
  val S01_AR_REG_TYPE: Int = 0,
  val S01_R_REG_TYPE: Int = 2,
  val M00_AW_REG_TYPE: Int = 1,
  val M00_W_REG_TYPE: Int = 2,
  val M00_B_REG_TYPE: Int = 0,
  val M00_AR_REG_TYPE: Int = 1,
  val M00_R_REG_TYPE: Int = 0
) extends BlackBox (
  Map(
    "DATA_WIDTH" -> DATA_WIDTH,
    "ADDR_WIDTH" -> ADDR_WIDTH,
    "STRB_WIDTH" -> (DATA_WIDTH / 8),
    "S_ID_WIDTH" -> S_ID_WIDTH,
    "M_ID_WIDTH" -> (S_ID_WIDTH + log2Ceil(S_COUNT)),
    "AWUSER_ENABLE" -> AWUSER_ENABLE,
    "AWUSER_WIDTH" -> AWUSER_WIDTH,
    "WUSER_ENABLE" -> WUSER_ENABLE,
    "WUSER_WIDTH" -> WUSER_WIDTH,
    "BUSER_ENABLE" -> BUSER_ENABLE,
    "BUSER_WIDTH" -> BUSER_WIDTH,
    "ARUSER_ENABLE" -> ARUSER_ENABLE,
    "ARUSER_WIDTH" -> ARUSER_WIDTH,
    "RUSER_ENABLE" -> RUSER_ENABLE,
    "RUSER_WIDTH" -> RUSER_WIDTH,
    "S00_THREADS" -> S00_THREADS,
    "S00_ACCEPT" -> S00_ACCEPT,
    "S01_THREADS" -> S01_THREADS,
    "S01_ACCEPT" -> S01_ACCEPT,
    "M_REGIONS" -> M_REGIONS,
    "M00_BASE_ADDR" -> M00_BASE_ADDR,
    "M00_ADDR_WIDTH" -> M00_ADDR_WIDTH,
    "M00_CONNECT_READ" -> M00_CONNECT_READ,
    "M00_CONNECT_WRITE" -> M00_CONNECT_WRITE,
    "M00_ISSUE" -> M00_ISSUE,
    "M00_SECURE" -> M00_SECURE,
    "S00_AW_REG_TYPE" -> S00_AW_REG_TYPE,
    "S00_W_REG_TYPE" -> S00_W_REG_TYPE,
    "S00_B_REG_TYPE" -> S00_B_REG_TYPE,
    "S00_AR_REG_TYPE" -> S00_AR_REG_TYPE,
    "S00_R_REG_TYPE" -> S00_R_REG_TYPE,
    "S01_AW_REG_TYPE" -> S01_AW_REG_TYPE,
    "S01_W_REG_TYPE" -> S01_W_REG_TYPE,
    "S01_B_REG_TYPE" -> S01_B_REG_TYPE,
    "S01_AR_REG_TYPE" -> S01_AR_REG_TYPE,
    "S01_R_REG_TYPE" -> S01_R_REG_TYPE,
    "M00_AW_REG_TYPE" -> M00_AW_REG_TYPE,
    "M00_W_REG_TYPE" -> M00_W_REG_TYPE,
    "M00_B_REG_TYPE" -> M00_B_REG_TYPE,
    "M00_AR_REG_TYPE" -> M00_AR_REG_TYPE,
    "M00_R_REG_TYPE" -> M00_R_REG_TYPE
  )
)  with HasBlackBoxResource  {
  val M_ID_WIDTH = S_ID_WIDTH + log2Ceil(S_COUNT)
  val STRB_WIDTH = DATA_WIDTH / 8

  val io = IO(new Bundle {
    val clk = Input(Clock())
    val rst = Input(Bool())

    // Slave interfaces (s00, s01)
    val s00_axi_awid    = Input(UInt(S_ID_WIDTH.W))
    val s00_axi_awaddr  = Input(UInt(ADDR_WIDTH.W))
    val s00_axi_awlen   = Input(UInt(8.W))
    val s00_axi_awsize  = Input(UInt(3.W))
    val s00_axi_awburst = Input(UInt(2.W))
    val s00_axi_awlock  = Input(Bool())
    val s00_axi_awcache = Input(UInt(4.W))
    val s00_axi_awprot  = Input(UInt(3.W))
    val s00_axi_awqos   = Input(UInt(4.W))
    val s00_axi_awuser  = Input(UInt(AWUSER_WIDTH.W))
    val s00_axi_awvalid = Input(Bool())
    val s00_axi_awready = Output(Bool())

    val s00_axi_wdata   = Input(UInt(DATA_WIDTH.W))
    val s00_axi_wstrb   = Input(UInt(STRB_WIDTH.W))
    val s00_axi_wlast   = Input(Bool())
    val s00_axi_wuser   = Input(UInt(WUSER_WIDTH.W))
    val s00_axi_wvalid  = Input(Bool())
    val s00_axi_wready  = Output(Bool())

    val s00_axi_bid     = Output(UInt(S_ID_WIDTH.W))
    val s00_axi_bresp   = Output(UInt(2.W))
    val s00_axi_buser   = Output(UInt(BUSER_WIDTH.W))
    val s00_axi_bvalid  = Output(Bool())
    val s00_axi_bready  = Input(Bool())

    val s00_axi_arid    = Input(UInt(S_ID_WIDTH.W))
    val s00_axi_araddr  = Input(UInt(ADDR_WIDTH.W))
    val s00_axi_arlen   = Input(UInt(8.W))
    val s00_axi_arsize  = Input(UInt(3.W))
    val s00_axi_arburst = Input(UInt(2.W))
    val s00_axi_arlock  = Input(Bool())
    val s00_axi_arcache = Input(UInt(4.W))
    val s00_axi_arprot  = Input(UInt(3.W))
    val s00_axi_arqos   = Input(UInt(4.W))
    val s00_axi_aruser  = Input(UInt(ARUSER_WIDTH.W))
    val s00_axi_arvalid = Input(Bool())
    val s00_axi_arready = Output(Bool())

    val s00_axi_rid     = Output(UInt(S_ID_WIDTH.W))
    val s00_axi_rdata   = Output(UInt(DATA_WIDTH.W))
    val s00_axi_rresp   = Output(UInt(2.W))
    val s00_axi_rlast   = Output(Bool())
    val s00_axi_ruser   = Output(UInt(RUSER_WIDTH.W))
    val s00_axi_rvalid  = Output(Bool())
    val s00_axi_rready  = Input(Bool())

    // s01 same as s00, repeat ports for s01
    val s01_axi_awid    = Input(UInt(S_ID_WIDTH.W))
    val s01_axi_awaddr  = Input(UInt(ADDR_WIDTH.W))
    val s01_axi_awlen   = Input(UInt(8.W))
    val s01_axi_awsize  = Input(UInt(3.W))
    val s01_axi_awburst = Input(UInt(2.W))
    val s01_axi_awlock  = Input(Bool())
    val s01_axi_awcache = Input(UInt(4.W))
    val s01_axi_awprot  = Input(UInt(3.W))
    val s01_axi_awqos   = Input(UInt(4.W))
    val s01_axi_awuser  = Input(UInt(AWUSER_WIDTH.W))
    val s01_axi_awvalid = Input(Bool())
    val s01_axi_awready = Output(Bool())

    val s01_axi_wdata   = Input(UInt(DATA_WIDTH.W))
    val s01_axi_wstrb   = Input(UInt(STRB_WIDTH.W))
    val s01_axi_wlast   = Input(Bool())
    val s01_axi_wuser   = Input(UInt(WUSER_WIDTH.W))
    val s01_axi_wvalid  = Input(Bool())
    val s01_axi_wready  = Output(Bool())

    val s01_axi_bid     = Output(UInt(S_ID_WIDTH.W))
    val s01_axi_bresp   = Output(UInt(2.W))
    val s01_axi_buser   = Output(UInt(BUSER_WIDTH.W))
    val s01_axi_bvalid  = Output(Bool())
    val s01_axi_bready  = Input(Bool())

    val s01_axi_arid    = Input(UInt(S_ID_WIDTH.W))
    val s01_axi_araddr  = Input(UInt(ADDR_WIDTH.W))
    val s01_axi_arlen   = Input(UInt(8.W))
    val s01_axi_arsize  = Input(UInt(3.W))
    val s01_axi_arburst = Input(UInt(2.W))
    val s01_axi_arlock  = Input(Bool())
    val s01_axi_arcache = Input(UInt(4.W))
    val s01_axi_arprot  = Input(UInt(3.W))
    val s01_axi_arqos   = Input(UInt(4.W))
    val s01_axi_aruser  = Input(UInt(ARUSER_WIDTH.W))
    val s01_axi_arvalid = Input(Bool())
    val s01_axi_arready = Output(Bool())

    val s01_axi_rid     = Output(UInt(S_ID_WIDTH.W))
    val s01_axi_rdata   = Output(UInt(DATA_WIDTH.W))
    val s01_axi_rresp   = Output(UInt(2.W))
    val s01_axi_rlast   = Output(Bool())
    val s01_axi_ruser   = Output(UInt(RUSER_WIDTH.W))
    val s01_axi_rvalid  = Output(Bool())
    val s01_axi_rready  = Input(Bool())

    // Master interface m00
    val m00_axi_awid    = Output(UInt(M_ID_WIDTH.W))
    val m00_axi_awaddr  = Output(UInt(ADDR_WIDTH.W))
    val m00_axi_awlen   = Output(UInt(8.W))
    val m00_axi_awsize  = Output(UInt(3.W))
    val m00_axi_awburst = Output(UInt(2.W))
    val m00_axi_awlock  = Output(Bool())
    val m00_axi_awcache = Output(UInt(4.W))
    val m00_axi_awprot  = Output(UInt(3.W))
    val m00_axi_awqos   = Output(UInt(4.W))
    val m00_axi_awregion= Output(UInt(4.W))
    val m00_axi_awuser  = Output(UInt(AWUSER_WIDTH.W))
    val m00_axi_awvalid = Output(Bool())
    val m00_axi_awready = Input(Bool())

    val m00_axi_wdata   = Output(UInt(DATA_WIDTH.W))
    val m00_axi_wstrb   = Output(UInt(STRB_WIDTH.W))
    val m00_axi_wlast   = Output(Bool())
    val m00_axi_wuser   = Output(UInt(WUSER_WIDTH.W))
    val m00_axi_wvalid  = Output(Bool())
    val m00_axi_wready  = Input(Bool())

    val m00_axi_bid     = Input(UInt(M_ID_WIDTH.W))
    val m00_axi_bresp   = Input(UInt(2.W))
    val m00_axi_buser   = Input(UInt(BUSER_WIDTH.W))
    val m00_axi_bvalid  = Input(Bool())
    val m00_axi_bready  = Output(Bool())

    val m00_axi_arid    = Output(UInt(M_ID_WIDTH.W))
    val m00_axi_araddr  = Output(UInt(ADDR_WIDTH.W))
    val m00_axi_arlen   = Output(UInt(8.W))
    val m00_axi_arsize  = Output(UInt(3.W))
    val m00_axi_arburst = Output(UInt(2.W))
    val m00_axi_arlock  = Output(Bool())
    val m00_axi_arcache = Output(UInt(4.W))
    val m00_axi_arprot  = Output(UInt(3.W))
    val m00_axi_arqos   = Output(UInt(4.W))
    val m00_axi_arregion= Output(UInt(4.W))
    val m00_axi_aruser  = Output(UInt(ARUSER_WIDTH.W))
    val m00_axi_arvalid = Output(Bool())
    val m00_axi_arready = Input(Bool())

    val m00_axi_rid     = Input(UInt(M_ID_WIDTH.W))
    val m00_axi_rdata   = Input(UInt(DATA_WIDTH.W))
    val m00_axi_rresp   = Input(UInt(2.W))
    val m00_axi_rlast   = Input(Bool())
    val m00_axi_ruser   = Input(UInt(RUSER_WIDTH.W))
    val m00_axi_rvalid  = Input(Bool())
    val m00_axi_rready  = Output(Bool())
  })

    addResource("/axi_crossbar.v")
    addResource("/axi_crossbar_addr.v")
    addResource("/axi_crossbar_wrap_2x1.v")
    addResource("/axi_crossbar_rd.v")
    addResource("/axi_crossbar_wr.v")
    addResource("/axi_register.v")
    addResource("/axi_register_rd.v")
    addResource("/axi_register_wr.v")
    addResource("/arbiter.v")
    addResource("/priority_encoder.v")
}
