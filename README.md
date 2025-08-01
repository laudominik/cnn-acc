Deep Learning Accelerator for CNNs
=======================

## Generating verilog

```sh
$ sbt "runMain AcceleratorDriver" 2> generated.v
```

## Synthesis

```sh
$ yosys script/synth.ys
```

## Running simulations

Testbenches in Chisel

```sh
$ sbt test
```

## Building docs

```sh
$ cd doc
$ make
```


### Resources
- https://github.com/lirui-shanghaitech/CNN-Accelerator-VLSI
