import chisel3._
import chisel3.util._

import chisel3._
import chisel3.util._

class BankedBuffer(val numBanks: Int, val bankDepth: Int, val width: Int) extends Module {
  val addrWidth = log2Ceil(bankDepth)

  val io = IO(new Bundle {
    val wrEn   = Input(Vec(numBanks, Bool()))
    val wrAddr = Input(Vec(numBanks, UInt(addrWidth.W)))
    val wrData = Input(Vec(numBanks, UInt(width.W)))

    val rdEn   = Input(Vec(numBanks, Bool()))
    val rdAddr = Input(Vec(numBanks, UInt(addrWidth.W)))
    val rdData = Output(Vec(numBanks, UInt(width.W)))
  })

  val readData = Wire(Vec(numBanks, UInt(width.W)))
  val banks = Seq.fill(numBanks) {
    SyncReadMem(bankDepth, UInt(width.W))
  }

  for (i <- 0 until numBanks) {
    when (io.wrEn(i)) {
      banks(i).write(io.wrAddr(i), io.wrData(i))
    }
  }
  for (i <- 0 until numBanks) {
    readData(i) := banks(i).read(io.rdAddr(i), io.rdEn(i))
  }
  io.rdData := readData
}
