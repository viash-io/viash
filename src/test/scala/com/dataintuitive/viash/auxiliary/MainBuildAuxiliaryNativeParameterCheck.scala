package com.dataintuitive.viash.auxiliary

import org.scalatest.{BeforeAndAfterAll, FunSuite}
import java.nio.file.Paths

import com.dataintuitive.viash.config.Config

import scala.io.Source
import com.dataintuitive.viash.helpers._
import com.dataintuitive.viash.TestHelper

class MainBuildAuxiliaryNativeParameterCheck extends FunSuite with BeforeAndAfterAll {
  // which platform to test
  private val configFile = getClass.getResource(s"/testbash/config.vsh.yaml").getPath

  private val temporaryFolder = IO.makeTemp("viash_tester")
  private val tempFolStr = temporaryFolder.toString

  // parse functionality from file
  private val functionality = Config.read(configFile, applyPlatform = false).functionality

  // check whether executable was created
  private val executable = Paths.get(tempFolStr, functionality.name).toFile

  // convert testbash
  test("viash can create the executable") {
    TestHelper.testMain(
      "build",
      "-p", "native",
      "-o", tempFolStr,
      configFile
    )

    assert(executable.exists)
    assert(executable.canExecute)
  }

  test("Check whether the executable can run") {
    Exec.run(
      Seq(executable.toString, "--help")
    )
  }

  test("Check whether particular keywords can be found in the usage") {
    val stdout =
      Exec.run(
        Seq(executable.toString, "--help")
      )

    val stripAll = (s : String) => s.replaceAll(raw"\s+", " ").trim

    functionality.allArguments.foreach(arg => {
      for (opt <- arg.alternatives; value <- opt)
        assert(stdout.contains(value))
      for (description <- arg.description) {
        assert(stripAll(stdout).contains(stripAll(description)))
      }
    })
  }

  test("Check whether double values are checked correctly") {
    // Decision was made to allow quite permissive syntax checking
    val checksPass = Seq("0", "1", "1.5", ".7", "1.5e5", "2.6e+6", "3.8e-7")
    val checksFail = Seq("1+", "1-", "1+2", "1-2", "foo", "123foo", "123.foo", "12e0.5")
    val signs = Seq("", "+", "-")

    for(
      value <- checksPass;
      sign <- signs
    ) {
      val param = sign + value
      val out = Exec.run2(
        Seq(
          executable.toString,
          executable.toString,
          "--real_number", param,
          "--whole_number=10",
          "-s", "string",
        )
      )
      assert(out.exitValue == 0, s"Test real_number: $param should pass")
    }

    for(
      value <- checksFail;
      sign <- signs
    ) {
      val param = sign + value
      val out = Exec.run2(
        Seq(
          executable.toString,
          executable.toString,
          "--real_number", param,
          "--whole_number=10",
          "-s", "string",
        )
      )
      assert(out.exitValue != 0, s"Test real_number: $param should fail")
    }

  }

  test("Check whether integer values are checked correctly") {
    // Decision was made to allow quite permissive syntax checking
    val checksPass = Seq("0", "1", "456")
    val checksFail = Seq("1.5", ".7", "1.5e5", "2.6e+6", "3.8e-7", "1+", "1-", "1+2", "1-2", "foo", "123foo", "123.foo", "12e0.5")
    val signs = Seq("", "+", "-")

    for(
      value <- checksPass;
      sign <- signs
    ) {
      val param = sign + value
      val out = Exec.run2(
        Seq(
          executable.toString,
          executable.toString,
          "--real_number", "1.23",
          "--whole_number", param,
          "-s", "string",
        )
      )
      assert(out.exitValue == 0, s"Test whole_number: $param should pass")
    }

    for(
      value <- checksFail;
      sign <- signs
    ) {
      val param = sign + value
      val out = Exec.run2(
        Seq(
          executable.toString,
          executable.toString,
          "--real_number", "1.23",
          "--whole_number", param,
          "-s", "string",
        )
      )
      assert(out.exitValue != 0, s"Test whole_number: $param should fail")
    }

  }

  test("Check whether boolean values are checked correctly") {
    // Decision was made to allow quite permissive syntax checking
    val checksPass = Seq("true", "True", "TRUE", "false", "False", "FALSE", "yes", "Yes", "YES", "no", "No", "NO")
    val checksFail = Seq("0", "1", "foo", "tRuE", "fALSE")
    val signs = Seq("+", "-")

    for(
      param <- checksPass
    ) {
      val out = Exec.run2(
        Seq(
          executable.toString,
          executable.toString,
          "--real_number", "1.23",
          "--whole_number", "10",
          "-s", "string",
          "--reality", param,
        )
      )
      assert(out.exitValue == 0, s"Test reality: $param should pass")
    }

    for(
      param <- checksFail
    ) {
      val out = Exec.run2(
        Seq(
          executable.toString,
          executable.toString,
          "--real_number", "1.23",
          "--whole_number", "10",
          "-s", "string",
          "--reality", param
        )
      )
      assert(out.exitValue != 0, s"Test reality: $param should fail")
    }

    for(
      value <- checksPass ++ checksFail;
      sign <- signs
    ) {
      val param = sign + value
      val out = Exec.run2(
        Seq(
          executable.toString,
          executable.toString,
          "--real_number", "1.23",
          "--whole_number", "10",
          "-s", "string",
          "--reality", param
        )
      )
      assert(out.exitValue != 0, s"Test reality: $param should fail")
    }

  }

  override def afterAll() {
    IO.deleteRecursively(temporaryFolder)
  }
}