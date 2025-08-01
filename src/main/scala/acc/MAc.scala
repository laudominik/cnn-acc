import chisel3._
import chisel3.util._

class MAc(width: Int = 8) extends Module {
  val io = IO(new Bundle {
    val input     = Input(UInt(width.W))
    val weight    = Input(UInt(width.W))
    val accumulator = Input(UInt((2*width).W))
    val out = Output(UInt((2*width).W))
  })

  val mul = RegInit(0.U((2 * width).W))

  val prod = RegNext(io.input * io.weight)

  io.out := prod + io.accumulator
}
