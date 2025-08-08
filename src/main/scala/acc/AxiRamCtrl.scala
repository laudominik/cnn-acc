import chisel3._
import chisel3.util._

class AxiRamWrCtrlCmd(dataWidth: Int, addrWidth: Int) extends Bundle {
  val addr = UInt(addrWidth.W)
  val len  = UInt(8.W)
  val data = UInt(dataWidth.W)
  val wstrb   = UInt((dataWidth/8).W)
}

class AxiRamRdCtrlCmd(dataWidth: Int, addrWidth: Int) extends Bundle {
  val addr = UInt(addrWidth.W)
  val len  = UInt(dataWidth.W)
}

class AxiRamRdResp(dataWidth: Int, addrWidth: Int) extends Bundle {
  val data = UInt(dataWidth.W)
  val len = UInt(8.W)
}

class AxiRamCtrlIO(dataWidth: Int, addrWidth: Int, idWidth: Int = 8) extends Bundle {
  val wrCmd = Flipped(Decoupled(new AxiRamWrCtrlCmd(dataWidth, addrWidth)))
  val rdCmd = Flipped(Decoupled(new AxiRamRdCtrlCmd(dataWidth, addrWidth)))
  val rdResp = Decoupled(new AxiRamRdResp(dataWidth, addrWidth))
  val axi = new AxiMasterIO(dataWidth, addrWidth, idWidth)
}

class AxiRamWrCtrlIO(dataWidth: Int, addrWidth: Int, idWidth: Int = 8) extends Bundle {
  val cmd = Flipped(Decoupled(new AxiRamWrCtrlCmd(dataWidth, addrWidth)))
  val axi = new AxiMasterWrIO(dataWidth, addrWidth, idWidth)
}

class AxiRamRdCtrlIO(dataWidth: Int, addrWidth: Int, idWidth: Int = 8) extends Bundle {
  val cmd = Flipped(Decoupled(new AxiRamRdCtrlCmd(dataWidth, addrWidth)))
  val resp = Decoupled(new AxiRamRdResp(dataWidth, addrWidth))
  val axi = new AxiMasterRdIO(dataWidth, addrWidth, idWidth)
}

class AxiRamWrCtrl(
  dataWidth: Int,
  addrWidth: Int,
  idWidth: Int = 8,
  id: Int = 0
) extends Module {
  val io = IO(new AxiRamWrCtrlIO(dataWidth, addrWidth, idWidth))
  val sIdle :: sAw :: sW :: sB :: Nil = Enum(4)
  val cmd = Reg(new AxiRamWrCtrlCmd(dataWidth, addrWidth))
  val state = RegInit(sIdle)

  io.axi.aw.id      := id.U
  io.axi.aw.addr    := cmd.addr
  io.axi.aw.len     := cmd.len
  io.axi.aw.size    := log2Ceil(dataWidth / 8).U
  io.axi.aw.burst   := 1.U // 1 == INCR
  io.axi.aw.lock    := 0.U
  io.axi.aw.cache   := 0.U
  io.axi.aw.prot    := 0.U
  io.axi.aw.valid   := (state === sAw)
  io.axi.w.data     := cmd.data
  io.axi.w.strb     := cmd.wstrb
  io.axi.w.last     := true.B
  io.axi.w.valid    := (state === sW)
  io.axi.b.ready    := (state === sB)
  io.cmd.ready      := (state === sIdle)

  switch(state) {
    is(sIdle) {
      when (io.cmd.valid) {
        printf(p"[AxiRamWrCtrl::sIdle] received command\n")
        cmd := io.cmd.bits
        state := sAw
      }
    }
    is(sAw) {
      when(io.axi.aw.ready) {
        printf(p"[AxiRamWrCtrl::sAw] address sent\n")
        state := sW
      }
      
    }
    is(sW) {
      when(io.axi.w.ready) {
        printf(p"[AxiRamWrCtrl::sW] data sent\n")
        state := sB
      }

    }
    is(sB) {
      when(io.axi.b.valid) {
        printf(p"[AxiRamWrCtrl::sW] burst ack received\n")
        state := sIdle
      }
    }
  }
}

class AxiRamRdCtrl(
  dataWidth: Int,
  addrWidth: Int,
  idWidth: Int = 8,
  id: Int = 0
) extends Module {
  val io = IO(new AxiRamRdCtrlIO(dataWidth, addrWidth, idWidth))
  val dataWidthB = dataWidth / 8
  val sIdle :: sAr :: sR :: Nil = Enum(3)
  val state = RegInit(sIdle)
  val cmd = Reg(new AxiRamRdCtrlCmd(dataWidth, addrWidth))
  val beatCount = RegInit(0.U(8.W))

  io.axi.ar.id      := id.U
  io.axi.ar.addr    := cmd.addr
  io.axi.ar.len     := cmd.len(8, 0)
  io.axi.ar.size    := log2Ceil(dataWidthB).U
  io.axi.ar.burst   := "b01".U 
  io.axi.ar.lock    := 0.U
  io.axi.ar.cache   := 0.U
  io.axi.ar.prot    := 0.U
  io.axi.ar.valid   := (state === sAr)

  io.axi.r.ready    := (state === sR) && io.resp.ready

  io.cmd.ready      := (state === sIdle)
  io.resp.valid     := (state === sR) && io.axi.r.valid
  io.resp.bits.data := io.axi.r.data
  io.resp.bits.len  := cmd.len

  switch(state) {
    is(sIdle) {
      when(io.cmd.valid) {
        printf(p"[AxiRamRdCtrl::sIdle] Received command: addr=0x${Hexadecimal(io.cmd.bits.addr)}, len=${io.cmd.bits.len}\n")
        cmd := io.cmd.bits
        beatCount := 0.U
        state := sAr
      }
    }
    is(sAr) {
      when(io.axi.ar.valid && io.axi.ar.ready) {
        printf(p"[AxiRamRdCtrl::sAr] Address sent\n")
        state := sR
      }
    }
    is(sR) {
      when(io.axi.r.valid && io.resp.ready) {
        printf(p"[AxiRamRdCtrl::sR] Beat: ${beatCount} recv: 0x${Hexadecimal(io.axi.r.data)} (last? ${io.axi.r.last}) \n")
     
        when(io.axi.r.last && (cmd.len - beatCount) === 0.U ) {
          state := sIdle  
        }.elsewhen (io.axi.r.last) {
            cmd.addr := cmd.addr + dataWidthB.U * beatCount
            cmd.len := cmd.len - beatCount
        }.otherwise {
          beatCount := beatCount + 1.U
        }
      }
    }
  }
}

class AxiRamCtrl(
  dataWidth: Int,
  addrWidth: Int,
  idWidth: Int = 8,
  id: Int = 0
) extends Module {
  val io = IO(new AxiRamCtrlIO(dataWidth, addrWidth, idWidth))
  val writer = Module(new AxiRamWrCtrl(dataWidth, addrWidth, idWidth, id))
  val reader = Module(new AxiRamRdCtrl(dataWidth, addrWidth, idWidth, id))
  writer.io.cmd <> io.wrCmd
  writer.io.axi.aw <> io.axi.aw
  writer.io.axi.w <> io.axi.w
  writer.io.axi.b <> io.axi.b
  reader.io.axi.ar <> io.axi.ar
  reader.io.axi.r <> io.axi.r
  reader.io.cmd <> io.rdCmd
  reader.io.resp <> io.rdResp
}
