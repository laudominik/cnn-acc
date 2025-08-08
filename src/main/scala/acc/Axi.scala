import chisel3._
import chisel3.util._

class AxiAw(dataWidth: Int, addrWidth:Int, idWidth: Int) extends Bundle {
    val id     = Output(UInt(idWidth.W))
    val addr   = Output(UInt(addrWidth.W))
    val len    = Output(UInt(8.W))
    val size   = Output(UInt(3.W))
    val burst  = Output(UInt(2.W))
    val lock   = Output(Bool())
    val cache  = Output(UInt(4.W))
    val prot   = Output(UInt(3.W))
    val valid  = Output(Bool())
    val ready  = Input(Bool())
}

class AxiW(dataWidth: Int, addrWidth:Int, idWidth: Int) extends Bundle {
    val data    = Output(UInt(dataWidth.W))
    val strb    = Output(UInt((dataWidth / 8).W))
    val last    = Output(Bool())
    val valid   = Output(Bool())
    val ready   = Input(Bool())
}

class AxiB(dataWidth: Int, addrWidth:Int, idWidth: Int) extends Bundle {
    val id      = Input(UInt(idWidth.W))
    val resp    = Input(UInt(2.W))
    val valid   = Input(Bool())
    val ready   = Output(Bool())
}

class AxiAr(dataWidth: Int, addrWidth: Int, idWidth: Int) extends Bundle {
    val id     = Output(UInt(idWidth.W))
    val addr   = Output(UInt(addrWidth.W))
    val len    = Output(UInt(8.W))
    val size   = Output(UInt(3.W))
    val burst  = Output(UInt(2.W))
    val lock   = Output(Bool())
    val cache  = Output(UInt(4.W))
    val prot   = Output(UInt(3.W))
    val valid  = Output(Bool())
    val ready  = Input(Bool())
}

class AxiR(dataWidth: Int, addrWidth: Int, idWidth: Int) extends Bundle {
    val id      = Input(UInt(idWidth.W))
    val data    = Input(UInt(dataWidth.W))
    val resp    = Input(UInt(2.W))
    val last    = Input(Bool())
    val valid   = Input(Bool())
    val ready   = Output(Bool())
}

class AxiMasterIO(dataWidth: Int, addrWidth: Int, idWidth: Int) extends Bundle {
  val aw = new AxiAw(dataWidth, addrWidth, idWidth)
  val w  = new AxiW(dataWidth, addrWidth, idWidth)
  val b  = new AxiB(dataWidth, addrWidth, idWidth)
  val ar = new AxiAr(dataWidth, addrWidth, idWidth)
  val r  = new AxiR(dataWidth, addrWidth, idWidth)
}

class AxiMasterWrIO(dataWidth: Int, addrWidth: Int, idWidth: Int) extends Bundle {
  val aw = new AxiAw(dataWidth, addrWidth, idWidth)
  val w  = new AxiW(dataWidth, addrWidth, idWidth)
  val b  = new AxiB(dataWidth, addrWidth, idWidth)
}

class AxiMasterRdIO(dataWidth: Int, addrWidth: Int, idWidth: Int) extends Bundle {
  val ar = new AxiAr(dataWidth, addrWidth, idWidth)
  val r  = new AxiR(dataWidth, addrWidth, idWidth)
}
