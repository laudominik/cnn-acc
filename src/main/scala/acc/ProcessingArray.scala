import circt.stage.ChiselStage
import chisel3._
import chisel3.util._

// systolic array of MAcs
// accumulation happens along the columns
class ProcessingArray(rows: Int, cols: Int, width: Int = 8) extends Module {
  val io = IO(new Bundle {
    val inputs  = Input(Vec(cols, UInt(width.W)))
    val weights = Input(Vec(rows, UInt(width.W)))
    val out = Output(Vec(rows, UInt((2*width).W)))
  })

  val macs = Seq.fill(rows, cols)(Module(new MAc(width)))
  val accRegs = Seq.fill(rows, cols)(RegInit(0.U((2 * width).W)))

  for (r <- 0 until rows) {
    for (c <- 0 until cols) {
      macs(r)(c).io.input := io.inputs(c)
      macs(r)(c).io.weight := io.weights(r)

      if (c == 0) {
        macs(r)(c).io.accumulator := 0.U
      } else {
        macs(r)(c).io.accumulator := accRegs(r)(c - 1)
      }

      accRegs(r)(c) := macs(r)(c).io.out
    }
  }

  for (r <- 0 until rows) {
    io.out(r) := accRegs(r)(cols-1)
  }
}


object ProcessingArrayDriver extends App {
  System.err.println(
    ChiselStage.emitSystemVerilog(new ProcessingArray(4, 5), firtoolOpts = Array("-disable-all-randomization", "-strip-debug-info"))
  )
}
