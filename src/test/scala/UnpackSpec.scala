package fpu

import chisel3._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec

class Unpack32Spec extends AnyFlatSpec with ChiselScalatestTester {
    behavior of "Unpack32"

    it should "Unpack 32 bits" in {
        test (new Unpack32) { c => 
            val input = "b11010110011110000111100001010101".U

            c.io.in.poke(input)
            c.io.s.expect(1.U)
            c.io.e.expect("b10101100".U)
            c.io.f.expect("b111110000111100001010101".U)
        }
    }
}