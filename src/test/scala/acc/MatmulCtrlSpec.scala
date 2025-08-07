import chisel3._
import chisel3.simulator.scalatest.ChiselSim
import chisel3.util._
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import scala.util.Random

class MatmulCtrlSpec extends AnyFreeSpec with Matchers with ChiselSim {
  val latency = 1
  // DRAM
  val dramDepth = 1024 // 4 kB
  val dramAddrWidth = log2Ceil(dramDepth)
  val dramDataWidth = 32

  // TODO
  val inputAddrWidth = 32
  val weightsAddrWidth = 32

  // systolic
  val dataWidth = 8
  val rows = 5
  val cols = 5
  
  "MatmulCtrl should carry out sent commands" in {
    simulate(
      new Module {
        val io = IO(new Bundle {
          val cmd   = Flipped(Decoupled(new MatmulCommand(dramAddrWidth)))
          val resp = Decoupled(MatmulStatus())
          val dramInit = Flipped(new DramInit(dramAddrWidth, dramDataWidth))
        })
        val ctrl = Module(new MatmulCtrl(rows, cols, dramAddrWidth, dramDataWidth, inputAddrWidth, weightsAddrWidth, dataWidth))
        val systolic = Module(new SystolicArray(rows, cols, dataWidth))
        val dram = Module(new Dram(dramAddrWidth, dramDataWidth))
        
        systolic.io.inputs := ctrl.io.systolicInput
        systolic.io.weights := ctrl.io.systolicWeights
        ctrl.io.systolicOut := systolic.io.out
        ctrl.io.cmd <> io.cmd
        ctrl.io.resp <> io.resp
        ctrl.io.dramIO <> dram.io
        io.dramInit <> dram.init 
      }) { dut =>
        def writeToDram(addr: Int, data: Int): Unit = {
          dut.io.dramInit.data.poke(data.U)
          dut.io.dramInit.addr.poke(addr)
          dut.io.dramInit.en.poke(true.B)
          dut.clock.step()
          dut.io.dramInit.en.poke(false.B)
        }

        val inputBaseAddr = 0x0
        val weightsBaseAddr = 0x100
        val inputMatrix = Array.fill(rows * cols)(Random.nextInt(256))
        val weightsMatrix = Array.fill(rows * cols)(Random.nextInt(256))


        val addr = inputBaseAddr
        writeToDram(0x100, 10)

        // for ((value, i) <- inputMatrix.zipWithIndex) {
        //   writeToDram(inputBaseAddr + i, value)
        // }

        // Write weights matrix to DRAM
        // for ((value, i) <- weightsMatrix.zipWithIndex) {
        //   writeToDram(weightsBaseAddr + i, value)
        // }

        dut.io.cmd.bits.weightsAddr.poke(weightsBaseAddr)
        dut.io.cmd.bits.inputAddr.poke(inputBaseAddr)
        // dut.io.cmd.bits.weightsDim.w.poke()
        // dut.io.cmd.bits.weightsDim.h.poke()
        // dut.io.cmd.bits.inputDim.w.poke()
        // dut.io.cmd.bits.inputDim.h.poke()
        // dut.io.cmd.bits.batchSize.poke(2.U)

        dut.io.cmd.valid.poke(true)
        dut.clock.step(4)

    }
  }
}