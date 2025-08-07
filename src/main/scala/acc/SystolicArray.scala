import circt.stage.ChiselStage
import chisel3.stage.ChiselGeneratorAnnotation
import chisel3._
import chisel3.util._


// https://www.telesens.co/2018/07/30/systolic-architectures/
class SystolicArray(rows: Int, cols: Int, width: Int = 8) extends Module {
  
  val io = IO(new Bundle {
    val inputs  = Input(Vec(rows, UInt(width.W)))
    val weights = Input(Vec(rows, Vec(cols, UInt(width.W))))
    val out     = Output(Vec(cols, UInt((2*width).W)))
  })
  val inputRegs = Seq.fill(rows, cols)(RegInit(0.U((2 * width).W)))
  val macs = Seq.fill(rows, cols)(Module(new MAc(width)))

  for (r <- 0 until rows) {
    for (c <- 0 until cols) {
      macs(r)(c).io.weight := io.weights(r)(c)
      macs(r)(c).io.input := inputRegs(r)(c)
    }
  }

  // input flow
  for (r <- 0 until rows) {
    inputRegs(r)(0) := io.inputs(r)
    for (c <- 1 until cols) {
      inputRegs(r)(c) := inputRegs(r)(c-1)
    }
  }

  // accumulation
  for (c <- 0 until cols) {
    macs(0)(c).io.accumulator := 0.U
    for (r <- 1 until rows) {
      macs(r)(c).io.accumulator := macs(r-1)(c).io.out
    }
    io.out(c) := macs(rows-1)(c).io.out
  }
}

object SystolicArrayDriver extends App {
  ChiselStage.emitSystemVerilog(new SystolicArray(4, 5), 
  firtoolOpts = Array(
    "-disable-all-randomization", 
    "-strip-debug-info", 
    "-enable-layers=Verification",
    "-enable-layers=Verification.Assert",
    "-enable-layers=Verification.Assume",
    "-enable-layers=Verification.Cover",
    "-o", "generated.v")
  )
}