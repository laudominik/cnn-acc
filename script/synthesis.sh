#!/bin/sh
sbt "runMain ProcessingArrayDriver" && yosys script/synth.ys
