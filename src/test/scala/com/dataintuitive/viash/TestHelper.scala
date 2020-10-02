package com.dataintuitive.viash

import java.io.ByteArrayOutputStream

import org.scalatest.Matchers.assertThrows
import org.scalatest.Tag

import scala.reflect.ClassTag

object DockerTest extends Tag("com.dataintuitive.viash.DockerTest")

object NativeTest extends Tag("com.dataintuitive.viash.NativeTest")

object TestHelper {

  /**
   * Method to capture the console stdout generated by Main.main() so we can analyse what's being outputted to the console
   * As the capture prevents the stdout being printed to the console, we print it after the Main.main() is finished.
   * @param args all the arguments typically passed to Main.main()
   * @return an array of strings of all the output
   */
  def testMain(args: Array[String]) : String = {
    val os = new ByteArrayOutputStream()
    Console.withOut(os) {
      Main.main(args)
    }

    val stdout = os.toString
    // Console.print(stdout)
    stdout
  }

  /**
   * Method to capture the console stdout generated by Main.main() so we can analyse what's being outputted to the console
   * As the capture prevents the stdout being printed to the console, we print it after the Main.main() is finished.
   * Additionally it handles a thrown RuntimeException using assertThrows
   * @param args all the arguments typically passed to Main.main()
   * @return an array of strings of all the output
   */
  def testMainException[T <: AnyRef: ClassTag](args: Array[String]) : String = {
    val os = new ByteArrayOutputStream()
    assertThrows[T] {
      Console.withOut(os) {
        Main.main(args)
      }
    }

    val stdout = os.toString()
    // Console.print(stdout)
    stdout
  }
}