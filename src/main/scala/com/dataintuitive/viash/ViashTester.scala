package com.dataintuitive.viash

import functionality._
import targets._
import resources.Script

import java.nio.file.{Paths, Files}

object ViashTester {
  def testFunctionality(fun: Functionality, platform: Target) = {
    // generate executable for native target
    val exe = NativeTarget().modifyFunctionality(fun).resources.head

    // fetch tests
    val tests = fun.tests.getOrElse(Nil)
    val executableTests = tests.filter(_.isInstanceOf[Script]).map(_.asInstanceOf[Script])

    executableTests.map{ test =>
      val funonlytest = platform.modifyFunctionality(fun.copy(resources = List(test)))

      import com.dataintuitive.viash.functionality.resources.BashScript
      val pimpedTest = BashScript(
        name = Some(test.filename),
        text = funonlytest.resources.head.text
      )

      val funfinal = funonlytest.copy(
        resources = pimpedTest :: exe :: fun.resources.tail ::: tests.filter(_.filename != test.filename)
      )


      val dir = Files.createTempDirectory("viash_" + funfinal.name).toFile()
      Main.writeResources(funfinal.resources, funfinal.rootDir, dir)

      // execute with parameters
      val executable = Paths.get(dir.toString(), test.filename).toString()

      // run command, collect output
      import sys.process._
      import java.io._
      val stream = new ByteArrayOutputStream
      val writer = new PrintWriter(stream)
      val exitValue = Seq(executable).!(ProcessLogger(writer.println, writer.println))
      writer.close()
      val s = stream.toString

      (test.filename, exitValue, s)
    }
  }
}