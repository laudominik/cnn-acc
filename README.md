Deep Learning Accelerator for CNNs
=======================

## Generating verilog

```sh
$ sbt "runMain AcceleratorDriver" 2> generated.v
```

## Synthesis

```sh
yosys> read_verilog generated.v
yosys> synth -top Accelerator
yosys> show Accelerator
```

## Running simulations

Testbenches in Chisel

```sh
$ sbt test
```

Test with Verilator

```sh
$ svsim
```

## Building docs

```sh
$ cd doc
$ make
```


### Resources
- https://github.com/lirui-shanghaitech/CNN-Accelerator-VLSI
