import chisel3._
import circt.stage.ChiselStage

class Accelerator(width: Int = 8) extends Module {
  val array_rows = 5
  val array_cols = 5
  val inputBankDepth = 256
  val weightBankDepth = 256

  val io = IO(new Bundle {

  })

}

object AcceleratorDriver extends App {
  System.err.println(
    ChiselStage.emitSystemVerilog(
      new Accelerator, firtoolOpts = Array("-disable-all-randomization", "-strip-debug-info")
    )
  )
}
