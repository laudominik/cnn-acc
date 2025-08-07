import chisel3._
import chisel3.util._

class DramIO(addrWidth: Int, dataWidth: Int) extends Bundle {
  val addr = Output(UInt(addrWidth.W))
  val readEn = Output(Bool())
  val readData = Input(UInt(dataWidth.W))
  val writeEn = Output(Bool())
  val writeData = Output(UInt(dataWidth.W))
}

class DramInit(addrWidth: Int, dataWidth: Int) extends Bundle {
    val en = Output(Bool())
    val addr = Output(UInt(addrWidth.W))
    val data = Output(UInt(dataWidth.W))
}

class Dram(val depth: Int, val dataWidth: Int) extends Module {
  val addrWidth = log2Ceil(depth)
  val io = IO(Flipped(new DramIO(addrWidth, dataWidth)))
  val init = IO(Flipped(new DramInit(addrWidth, dataWidth)))

  val mem = SyncReadMem(depth, UInt(dataWidth.W))
  val readAddr = Reg(UInt(addrWidth.W))

  io.readData := DontCare
  when(io.readEn) {
    readAddr := io.addr
  }
  io.readData := mem.read(readAddr, io.readEn)
  
  when(init.en) {
    mem.write(init.addr, init.data)
  }.elsewhen(io.writeEn) {
    mem.write(io.addr, io.writeData)
  }
}
