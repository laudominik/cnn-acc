import circt.stage.ChiselStage
import chisel3.stage.ChiselGeneratorAnnotation
import chisel3._
import chisel3.util._


/*
 * e.g. 3x3 matrix allocation in DRAM
 *  ---> growing addresses
 * [a00, a01, a02, a10, a11, a12, a12, a21, a22]
 *
*/

class MatrixDim extends Bundle {
  val w = UInt(16.W)
  val h = UInt(16.W)
}

class MatmulCommand(dramAddrWidth: Int) extends Bundle {
  val weightsAddr = UInt(dramAddrWidth.W)
  val inputAddr = UInt(dramAddrWidth.W)
  val weightsDim = new MatrixDim
  val inputDim = new MatrixDim
  val batchSize = UInt(32.W)
}

object MatmulStatus extends ChiselEnum {
  val ok = Value(0.U)
  val error = Value(1.U)
}

class MatmulCtrl(
  // systolic 
  rows: Int, cols: Int,  
  // dram
  dramAddrWidth: Int, dramDataWidth: Int,
  // buffers
  inputAddrWidth: Int, weightsAddrWidth: Int, 
  // systolic
  dataWidth: Int = 8
) extends Module {
  val io = IO(new Bundle {
    // req/resp
    val cmd   = Flipped(Decoupled(new MatmulCommand(dramAddrWidth)))
    val resp = Decoupled(MatmulStatus())
    // systolic
    val systolicInput  = Output(Vec(rows, UInt(dataWidth.W)))
    val systolicWeights = Output(Vec(rows, Vec(cols, UInt(dataWidth.W))))
    val systolicOut     = Input(Vec(cols, UInt((2*dataWidth).W)))
    // axi ram (master)
    val axiRamWrCmd = Decoupled(new AxiRamWrCtrlCmd(dramDataWidth, dramAddrWidth))
    val axiRamRdCmd = Decoupled(new AxiRamRdCtrlCmd(dramDataWidth, dramAddrWidth))
    val axiRamRdResp = Flipped(Decoupled(new AxiRamRdResp(dramDataWidth, dramAddrWidth)))
  })
  val log = SimLog.StdErr
  val dramWidthB = dramDataWidth / 8
  val sIdle :: sInitWeightsRequest :: sInitWeights :: sBusy :: sDone :: Nil = Enum(5)
  val state = RegInit(sIdle)
  val cmd = RegInit(0.U.asTypeOf(new MatmulCommand(dramAddrWidth)))
  val inputRegs = RegInit(VecInit(Seq.fill(rows)(0.U(dataWidth.W))))
  val axiSystolicDataRatio = dramDataWidth / dataWidth

  // weights
  val addrReg = RegInit(0.U(dramAddrWidth.W))
  val lenReg = RegInit(0.U(dramDataWidth.W))

  val weightRegs = RegInit(VecInit(Seq.fill(rows)(VecInit(Seq.fill(cols)(0.U(dataWidth.W))))))
  val weightsCopied = Reg(UInt(16.W))
  val targetWeights = Reg(UInt(16.W))

  io.systolicInput := inputRegs
  io.systolicWeights := weightRegs
  io.resp.bits := MatmulStatus.ok

  io.cmd.nodeq()
  io.cmd.ready := (state === sIdle)
  io.resp.valid := (state === sDone && io.resp.ready)
 
  io.axiRamWrCmd.bits := 0.U.asTypeOf(new AxiRamWrCtrlCmd(dramDataWidth, dramAddrWidth))
  io.axiRamRdCmd.bits := 0.U.asTypeOf(new AxiRamRdCtrlCmd(dramDataWidth, dramAddrWidth))
  io.axiRamWrCmd.valid := 0.U
  io.axiRamRdResp.ready := (state === sInitWeights)


  io.axiRamRdCmd.bits.addr := addrReg
  io.axiRamRdCmd.bits.len := lenReg
  io.axiRamRdCmd.valid := (state === sInitWeightsRequest)

  def ceilUIntDiv(a: UInt, b: UInt): UInt =
    (a + b - 1.U) / b


  switch(state) {
    is(sIdle) {
      when(io.cmd.valid) {
        state := sInitWeightsRequest
        cmd := io.cmd.bits

        addrReg := io.cmd.bits.weightsAddr
  
        lenReg := ceilUIntDiv(io.cmd.bits.weightsDim.w * io.cmd.bits.weightsDim.h, axiSystolicDataRatio.U) - 1.U // how many beats
        printf(p"[MatmulCtrl::sIdle] Command received: ${io.cmd.bits}  \n")
      }
    }
    is(sInitWeightsRequest) {
      when (io.axiRamRdResp.valid) {
        printf(p"[MatmulCtrl::sInitWeightsRequest] -> sInitWeights\n")
        state := sInitWeights
      }
    }
    is(sInitWeights) {
      printf(p"[MatmulCtr::sInitWeights] \n")
      // weightsCopied := weightsCopied + 1.U
      // weightRegs(0)(0) := readData // TODO: slice bits
      // weights
      // io.dramIO.addr := 
      // copy weights
    }
    // is(sInitInputs) {
    // enqueue data
    // }
    is(sBusy) {
      // read data from buffers, send them to systolic array
    }
    is(sDone) {
      when(io.resp.ready) {
        io.resp.bits := MatmulStatus.ok
      }
      // when(io)
    }
  }
}

