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
  val rows = 8
  val cols = 8

  "MatmulCtrl should carry out sent commands" in {
    simulate(
      new Module {
        val io = IO(new Bundle {
          val cmd   = Flipped(Decoupled(new MatmulCommand(dramAddrWidth)))
          val testWrCmd = Flipped(Decoupled(new AxiRamWrCtrlCmd(dramDataWidth, dramAddrWidth)))
          val testRdCmd = Flipped(Decoupled(new AxiRamRdCtrlCmd(dramDataWidth, dramAddrWidth)))
          val testRdResp = Decoupled(new AxiRamRdResp(dramDataWidth, dramAddrWidth))
          val resp = Decoupled(MatmulStatus())
        })
        val ctrl = Module(new MatmulCtrl(rows, cols, dramAddrWidth, dramDataWidth, inputAddrWidth, weightsAddrWidth, dataWidth))
        val systolic = Module(new SystolicArray(rows, cols, dataWidth))
        val axiCtrl = Module(new AxiRamCtrl(dramDataWidth, dramAddrWidth))
        val testableRam = Module(new TestableAxiRam(dramDataWidth, dramAddrWidth))
        axiCtrl.io.axi <> testableRam.io.axi
        axiCtrl.io.wrCmd <> ctrl.io.axiRamWrCmd
        axiCtrl.io.rdCmd <> ctrl.io.axiRamRdCmd
        axiCtrl.io.rdResp <> ctrl.io.axiRamRdResp
        io.testWrCmd <> testableRam.io.testWrCmd
        io.testRdCmd <> testableRam.io.testRdCmd
        io.testRdResp <> testableRam.io.testRdResp

        systolic.io.inputs := ctrl.io.systolicInput
        systolic.io.weights := ctrl.io.systolicWeights
        ctrl.io.systolicOut := systolic.io.out
        ctrl.io.cmd <> io.cmd
        ctrl.io.resp <> io.resp
      }) { dut =>
        val inputBaseAddr = 0x0
        val weightsBaseAddr = 0x100
        val inputW = 5
        val inputH = 5
        val batchSize = 2
        val inputMatrix = Array.fill(rows * cols)(Random.nextInt(256))
        val weightsMatrix = Array.fill(rows * cols)(Random.nextInt(256))

        dut.reset.poke(true)
        dut.clock.step()
        dut.reset.poke(false)
        dut.clock.step()

        dut.io.cmd.bits.weightsAddr.poke(weightsBaseAddr)
        dut.io.cmd.bits.inputAddr.poke(inputBaseAddr)
        dut.io.cmd.bits.weightsDim.w.poke(inputW)
        dut.io.cmd.bits.weightsDim.h.poke(inputH)
        dut.io.cmd.bits.inputDim.w.poke(inputW)
        dut.io.cmd.bits.inputDim.h.poke(inputH)
        dut.io.cmd.bits.batchSize.poke(batchSize)
        dut.io.cmd.valid.poke(true)
        dut.clock.step(30)

    }
  }
}