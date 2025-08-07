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
    val dramIO          = new DramIO(dramAddrWidth, dramDataWidth)
    // dram
    // val dramRdEn        = Output(Bool())
    // val dramRdAddr      = Output(UInt(addrWidth))
    // val dramRdData      = Input(UInt(dataWidth))
  })
  val log = SimLog.StdErr
  val sIdle :: sInit :: sInitWeights :: sBusy :: sDone :: Nil = Enum(5)
  val state = RegInit(sIdle)
  val cmd = RegInit(0.U.asTypeOf(new MatmulCommand(dramAddrWidth)))
  val inputRegs = RegInit(VecInit(Seq.fill(rows)(0.U(dataWidth.W))))

  // weights
  val weightRegs = RegInit(VecInit(Seq.fill(rows)(VecInit(Seq.fill(cols)(0.U(dataWidth.W))))))
  val weightsCopied = Reg(UInt(16.W))
  val targetWeights = Reg(UInt(16.W))
  val readData = io.dramIO.readData

  io.systolicInput := inputRegs
  io.systolicWeights := weightRegs
  io.resp.bits := MatmulStatus.ok

  io.cmd.nodeq()
  io.cmd.ready := (state === sIdle)
  io.resp.valid := (state === sDone && io.resp.ready)
  io.dramIO.writeEn := 0.U
  io.dramIO.readEn := (
    state === sInit || state === sInitWeights
  )
  io.dramIO.addr := DontCare
  io.dramIO.readData := DontCare
  io.dramIO.writeData := DontCare

  switch(state) {
    is(sIdle) {
      when(io.cmd.valid) {
        state := sInit
        cmd := io.cmd.bits

        printf(p"[MatmulCtrl::sIdle] Command received\n")
      }
    }
    is(sInit) {
      printf(p"[MatmulCtrl::sInit] batchSize=${cmd.batchSize} inputAddr=${cmd.inputAddr}\n")
      state := sInitWeights
      weightsCopied := 0.U
      io.dramIO.addr := cmd.weightsAddr
    }
    is(sInitWeights) {
      printf(p"[MatmulCtr::sInitWeights] saving weight ${readData} \n")
      weightsCopied := weightsCopied + 1.U
      weightRegs(0)(0) := readData // TODO: slice bits
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


  // when(io.cmd.valid) {
  //   // io.cmd.ready 


  //   // io.read_routing_table_response.enq(tbl(
  //   //   io.read_routing_table_request.deq().addr
  //   // ))
  // }

  // when(cmd.ready)

}

