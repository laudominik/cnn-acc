import circt.stage.ChiselStage
import chisel3.stage.ChiselGeneratorAnnotation
import chisel3._
import chisel3.util._

// diagonal systolic array of MAcs
// accumulation happens along the columns
// input data is fed along the diagonals
// same weight is shared across a single column
// dimensions of an array: inputs = weights + cols - 2
class DiagonalProcessingArray(weight_cnt: Int, input_cnt: Int, width: Int = 8) extends Module {
  val cols = weight_cnt
  val rows = input_cnt + 1 - cols
  
  val io = IO(new Bundle {
    val inputs  = Input(Vec(input_cnt, UInt(width.W)))
    val weights = Input(Vec(weight_cnt, UInt(width.W)))
    val out = Output(Vec(rows, UInt((2*width).W)))
  })


  val macs = Seq.fill(rows, cols)(Module(new MAc(width)))
  val accRegs = Seq.fill(rows, cols)(RegInit(0.U((2 * width).W)))
  val inputRegs = Seq.fill(rows, cols)(RegInit(0.U((2 * width).W)))

  // input registers - systolic data flow along the diagonals towards "top right"
  for (r <- 1 until rows) {
    for (c <- 0 until cols-1) {
      inputRegs(r-1)(c+1) := RegNext(inputRegs(r)(c))
    }
  }
  for(r <- 0 until rows) {
    inputRegs(r)(0) := RegNext(io.inputs(r))
  }
  for (c <- 1 until cols) {
    inputRegs(rows - 1)(c) := RegNext(io.inputs(rows + c - 1))
  }

  for (r <- 0 until rows) {
    for (c <- 0 until cols) {
      macs(r)(c).io.input := inputRegs(r)(c)      
      macs(r)(c).io.weight := io.weights(c)
      accRegs(r)(c) := macs(r)(c).io.out
    }

    macs(r)(0).io.accumulator := 0.U
    for (c <- 1 until cols) {
        macs(r)(c).io.accumulator := accRegs(r)(c-1)
    }
  }

  for (r <- 0 until rows) {
    io.out(r) := accRegs(r)(cols-1)

  }
}

object DiagonalProcessingArrayDriver extends App {
  ChiselStage.emitSystemVerilog(new DiagonalProcessingArray(4, 5), 
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