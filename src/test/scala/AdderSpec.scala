package fpu

import chisel3._
import chiseltest._
import scala.math.{pow, log}
import scala.collection.mutable.ListBuffer
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers._
import chiseltest.simulator.WriteVcdAnnotation

class FastAdderPipelinedSpec extends AnyFlatSpec with ChiselScalatestTester {
    behavior of "FastAdderPipelined"

    val lnOf2 = scala.math.log(2) // natural log of 2
    def log2(x: Double): Double = scala.math.log(x) / lnOf2
    val r = scala.util.Random

    for (w <- List(8, 16, 32, 64)) {
        val hi = BigInt(2).pow(w)
        it should s"Add $w bit numbers" in {
            for (i <- 1 to 50) {
                test (new FastAdderPipelined(w)).withAnnotations(Seq(WriteVcdAnnotation)) { c =>
                    val a = BigInt(w, r)
                    val b = BigInt(w, r)
                    val cin = r.nextInt(2)
                    val cout = if (a + b + cin >=  hi) 1 else 0
                    val sum = (a+b+cin) & (hi-1) 
                    val steps = 3 + log2(w/4).toInt

                    c.io.a.poke(a.U)
                    c.io.b.poke(b.U)
                    c.io.cin.poke(cin.U)

                    c.clock.step(steps)

                    c.io.Sum.expect(sum.U)
                    c.io.Cout.expect(cout.U)
                }
            }
        }
    }

    for (w <- List(8, 16, 32, 64)) {
        val n = 20 
        it should s"Add $n pipelined $w-bit numbers" in {
            test (new FastAdderPipelined(w)).withAnnotations(Seq(WriteVcdAnnotation)) { c =>
                val hi = BigInt(2).pow(w)
                // 1 extra stage for group condense and another for carry-in sum
                val steps = 3 + log2(w/4).toInt
                val ips = for (i <- 1 to n) yield { 
                    val a = BigInt(w, r)
                    val b = BigInt(w, r)
                    val cin = r.nextInt(2)
                    val sum = (a+b+cin) & (hi-1) 
                    val cout = if (a + b + cin >=  hi) 1 else 0
                    (a, b, cin, sum, cout)
                }

                val outBuffer = new ListBuffer[(BigInt, BigInt)]()
                for (ip <- ips) {
                    c.io.a.poke(ip._1.U)
                    c.io.b.poke(ip._2.U)
                    c.io.cin.poke(ip._3.U)
                    val o = (c.io.Sum.peek().litValue, c.io.Cout.peek().litValue)
                    outBuffer += o
                    c.clock.step(1)
                }

                for (i <- 1 to steps) {
                    val o = (c.io.Sum.peek().litValue, c.io.Cout.peek().litValue)
                    outBuffer += o
                    c.clock.step(1)
                }

                val outList = outBuffer.toList.drop(outBuffer.size - n)
                ips.zip(outList).map{ case (ip, op) => (ip._4, ip._5) shouldBe (op._1, op._2) }
            }
        }
    }
}