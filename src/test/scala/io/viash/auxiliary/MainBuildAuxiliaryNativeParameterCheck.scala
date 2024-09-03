package io.viash.auxiliary

import org.scalatest.BeforeAndAfterAll
import org.scalatest.funsuite.AnyFunSuite
import java.nio.file.{Paths, Files, StandardCopyOption}

import io.viash.config.Config

import scala.io.Source
import io.viash.helpers.{IO, Exec, Logger}
import io.viash.TestHelper
import java.nio.file.Path
import scala.annotation.meta.param

class MainBuildAuxiliaryNativeParameterCheck extends AnyFunSuite with BeforeAndAfterAll {
  Logger.UseColorOverride.value = Some(false)
  // which configs to test
  private val configFile = getClass.getResource("/testbash/auxiliary_requirements/parameter_check.vsh.yaml").getPath
  private val loopConfigFile = getClass.getResource("/testbash/auxiliary_requirements/parameter_check_loop.vsh.yaml").getPath

  private val temporaryFolder = IO.makeTemp("viash_tester")
  private val tempFolStr = temporaryFolder.toString

  // parse config from file
  private val config = Config.read(configFile)
  private val loopConfig = Config.read(loopConfigFile)

  // check whether executable was created
  private val executable = Paths.get(tempFolStr, config.name).toFile
  private val loopExecutable = Paths.get(tempFolStr, loopConfig.name).toFile

  def generatePassAndFail(passSigns: Seq[String], passValues: Seq[String], failSigns: Seq[String], failValues: Seq[String]) = {
    assert(passSigns.length > 0)
    assert(passValues.length > 0)
    assert(failSigns.length > 0)
    assert(failValues.length > 0)

    val passSingles = for(ps <- passSigns; pv <- passValues) yield ps + pv
    val failSingles = for(fs <- failSigns; fv <- failValues) yield fs + fv

    val passCombined = for(
      c <- 1 until 3;
      v <- passSingles.combinations(c)
    ) yield v.mkString(";")
    val failCombined = for(
      c <- 1 until 3;
      v <- failSingles.combinations(c)
    ) yield v.mkString(";")
    val failMixCombined = for(
      a <- 1 until 3;
      b <- 1 until 3;
      p <- passSingles.take(4).combinations(a); // limit amount of tests with .take, this becomes big *fast*
      f <- failSingles.take(4).combinations(b); // limit amount of tests with .take, this becomes big *fast*
      order <- Seq(true, false)
    ) yield if (order) (p++f).mkString(";") else (f++p).mkString(";")
    (passSingles, failSingles, passCombined, failCombined++failMixCombined)
  }

  def testSingleParameter(parameterName: String, valuesAndExpectedResults: Seq[(String, Int)], testName: String) = {
    val values = valuesAndExpectedResults.map(_._1)
    val inputFile = Paths.get(tempFolStr, testName + "_input.txt")
    val outputFile = Paths.get(tempFolStr, testName + "_output.txt")
    IO.write(values.mkString("\n")+"\n", inputFile, true, None)

    val out = Exec.runCatch(
      Seq(
        loopExecutable.toString,
        "--exec_path", executable.toString,
        "--input", inputFile.toString,
        "--output", outputFile.toString,
        "--parameter", parameterName
      )
    )

    val results = Source.fromURI(outputFile.toUri).getLines().toList
    val combinedResults = valuesAndExpectedResults.zipAll(results, ("no value", -1), -1).map{ case (((a, b), c)) => (a, b, c) }

    combinedResults.foreach{ case (input, expected, result) => 
      assert(expected.toString == result, s" Test failed for --$parameterName: $input\n$result")
    }

  }

  // convert testbash
  test("viash can create the executable") {
    TestHelper.testMain(
      "build",
      "--engine", "native",
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

  test("viash can create the loop executable") {
    TestHelper.testMain(
      "build",
      "--engine", "native",
      "-o", tempFolStr,
      loopConfigFile
    )

    assert(loopExecutable.exists)
    assert(loopExecutable.canExecute)
  }

  test("Check whether the loop executable can run") {
    Exec.run(
      Seq(loopExecutable.toString, "--help")
    )
  }

  test("Check whether particular keywords can be found in the usage") {
    val stdout =
      Exec.run(
        Seq(executable.toString, "--help")
      )

    val stripAll = (s : String) => s.replaceAll(raw"\s+", " ").trim

    config.allArguments.foreach(arg => {
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

    val (passSingle, failSingle, passMulti, failMulti) = generatePassAndFail(signs, checksPass, signs, checksFail)

    val singleTests = passSingle.map((_, 0)) ++ failSingle.map((_, 1))
    val multiTests = passMulti.map((_, 0)) ++ failMulti.map((_, 1))

    testSingleParameter("real_number", singleTests, "double_single")
    testSingleParameter("real_number_multiple", multiTests, "double_multi")
  }

  test("Check whether integer values are checked correctly") {
    // Decision was made to allow quite permissive syntax checking
    val checksPass = Seq("0", "1", "456")
    val checksFail = Seq("1.5", ".7", "1.5e5", "2.6e+6", "3.8e-7", "1+", "1-", "1+2", "1-2", "foo", "123foo", "123.foo", "12e0.5")
    val signs = Seq("", "+", "-")

    val (passSingle, failSingle, passMulti, failMulti) = generatePassAndFail(signs, checksPass, signs, checksFail)

    val singleTests = passSingle.map((_, 0)) ++ failSingle.map((_, 1))
    val multiTests = passMulti.map((_, 0)) ++ failMulti.map((_, 1))

    testSingleParameter("whole_number", singleTests, "integer_single")
    testSingleParameter("whole_number_multiple", multiTests, "integer_multi")
  }

  test("Check whether long values are checked correctly") {
    // Decision was made to allow quite permissive syntax checking
    val checksPass = Seq("0", "1", "456")
    val checksFail = Seq("1.5", ".7", "1.5e5", "2.6e+6", "3.8e-7", "1+", "1-", "1+2", "1-2", "foo", "123foo", "123.foo", "12e0.5")
    val signs = Seq("", "+", "-")

    val (passSingle, failSingle, passMulti, failMulti) = generatePassAndFail(signs, checksPass, signs, checksFail)

    val singleTests = passSingle.map((_, 0)) ++ failSingle.map((_, 1))
    val multiTests = passMulti.map((_, 0)) ++ failMulti.map((_, 1))

    testSingleParameter("long_number", singleTests, "long_single")
    testSingleParameter("long_number_multiple", multiTests, "long_multi")
  }

  test("Check whether boolean values are checked correctly") {
    // Decision was made to allow quite permissive syntax checking
    val checksPass = Seq("true", "True", "TRUE", "false", "False", "FALSE", "yes", "Yes", "YES", "no", "No", "NO")
    val checksFail = Seq("0", "1", "foo", "tRuE", "fALSE")
    val signs = Seq("", "+", "-")

    val (passSingle, failSingle, passMulti, failMulti) = generatePassAndFail(Seq(""), checksPass, signs, checksFail)

    val singleTests = passSingle.map((_, 0)) ++ failSingle.map((_, 1))
    val multiTests = passMulti.map((_, 0)) ++ failMulti.map((_, 1))

    testSingleParameter("reality", singleTests, "boolean_single")
    testSingleParameter("reality_multiple", multiTests, "boolean_multi")

    val singleSignTests = checksPass.map(b => ("+" + b, 1)) ++ checksPass.map(b => ("-" + b, 1))

    testSingleParameter("reality", singleSignTests, "boolean_single_signs")

  }

  test("Check whether string values with allowed values are checked correctly") {
    // Decision was made to allow quite permissive syntax checking
    val checksPass = Seq("one", "2", "two words", "three separate words", 
      "Two full blown sentences with punctuation. The weather is nice today!", "Capital",
      "ALL CAPITALS", "a", "B")
    val checksFail = Seq("0", "1", "foo", "bone", "one 2")

    val (passSingle, failSingle, passMulti, failMulti) = generatePassAndFail(Seq(""), checksPass, Seq(""), checksFail)

    val singleTests = passSingle.map((_, 0)) ++ failSingle.map((_, 1))
    val multiTests = passMulti.map((_, 0)) ++ failMulti.map((_, 1))

    testSingleParameter("string", singleTests, "string_single")
    testSingleParameter("multiple", multiTests, "string_multi")
  }

  test("Check whether integer values with allowed values are checked correctly") {
    val checksPass = Seq("0", "1", "3", "-10")
    val checksFail = Seq("2", "-1", "foo")

    val (passSingle, failSingle, passMulti, failMulti) = generatePassAndFail(Seq(""), checksPass, Seq(""), checksFail)

    val singleTests = passSingle.map((_, 0)) ++ failSingle.map((_, 1))
    val multiTests = passMulti.map((_, 0)) ++ failMulti.map((_, 1))

    testSingleParameter("whole_number_choice", singleTests, "whole_number_choice_single")
    testSingleParameter("whole_number_choice_multiple", multiTests, "whole_number_choice_multi")
  }

  test("Check whether long values with allowed values are checked correctly") {
    val checksPass = Seq("0", "1", "3000000000", "-9876543210")
    val checksFail = Seq("2", "-1", "foo")

    val (passSingle, failSingle, passMulti, failMulti) = generatePassAndFail(Seq(""), checksPass, Seq(""), checksFail)

    val singleTests = passSingle.map((_, 0)) ++ failSingle.map((_, 1))
    val multiTests = passMulti.map((_, 0)) ++ failMulti.map((_, 1))

    testSingleParameter("long_number_choice", singleTests, "long_number_choice_single")
    testSingleParameter("long_number_choice_multiple", multiTests, "long_number_choice_multi")
  }

  test("Check whether integer values with min and/or max specified are checked correctly") {
    // min -3
    // max 5
    // traditionally bad arguments should already be checked elsewhere, so do less of them here
    val checkMin = Seq("-10", "-4")
    val checkMax = Seq("6", "10")
    val checksPass = Seq("-3", "0", "2", "5")
    val checksFail = Seq("1.5", ".7", "1.5e5", "foo")
    val signs = Seq("")

    {
      val (passSingle, failSingle, passMulti, failMulti) = generatePassAndFail(signs, checksPass ++ checkMax, signs, checksFail ++ checkMin)

      val singleTests = passSingle.map((_, 0)) ++ failSingle.map((_, 1))
      val multiTests = passMulti.map((_, 0)) ++ failMulti.map((_, 1))

      testSingleParameter("whole_number_min", singleTests, "whole_number_min_single")
      testSingleParameter("whole_number_min_multiple", multiTests, "whole_number_min_multi")
    }

    {
      val (passSingle, failSingle, passMulti, failMulti) = generatePassAndFail(signs, checksPass ++ checkMin, signs, checksFail ++ checkMax)

      val singleTests = passSingle.map((_, 0)) ++ failSingle.map((_, 1))
      val multiTests = passMulti.map((_, 0)) ++ failMulti.map((_, 1))

      testSingleParameter("whole_number_max", singleTests, "whole_number_max_single")
      testSingleParameter("whole_number_max_multiple", multiTests, "whole_number_max_multi")
    }

    {
      val (passSingle, failSingle, passMulti, failMulti) = generatePassAndFail(signs, checksPass, signs, checksFail ++ checkMin ++ checkMax)

      val singleTests = passSingle.map((_, 0)) ++ failSingle.map((_, 1))
      val multiTests = passMulti.map((_, 0)) ++ failMulti.map((_, 1))

      testSingleParameter("whole_number_min_max", singleTests, "whole_number_min_max_single")
      testSingleParameter("whole_number_min_max_multiple", multiTests, "whole_number_min_max_multi")
    }

  }

  test("Check whether double values with min and/or max specified are checked correctly") {
    // min -3.2
    // max 5.7
    // traditionally bad arguments should already be checked elsewhere, so do less of them here
    val checkMin = Seq("-10", "-4", "-3.3")
    val checkMax = Seq("5.8", "6", "10")
    val checksPass = Seq("-3.2", "0", "2", "5.7")
    val checksFail = Seq("1+", "1-", "foo")
    val signs = Seq("")

    {
      val (passSingle, failSingle, passMulti, failMulti) = generatePassAndFail(signs, checksPass ++ checkMax, signs, checksFail ++ checkMin)

      val singleTests = passSingle.map((_, 0)) ++ failSingle.map((_, 1))
      val multiTests = passMulti.map((_, 0)) ++ failMulti.map((_, 1))

      testSingleParameter("real_number_min", singleTests, "real_number_min_single")
      testSingleParameter("real_number_min_multiple", multiTests, "real_number_min_multi")
    }

    {
      val (passSingle, failSingle, passMulti, failMulti) = generatePassAndFail(signs, checksPass ++ checkMin, signs, checksFail ++ checkMax)

      val singleTests = passSingle.map((_, 0)) ++ failSingle.map((_, 1))
      val multiTests = passMulti.map((_, 0)) ++ failMulti.map((_, 1))

      testSingleParameter("real_number_max", singleTests, "real_number_max_single")
      testSingleParameter("real_number_max_multiple", multiTests, "real_number_max_multi")
    }

    {
      val (passSingle, failSingle, passMulti, failMulti) = generatePassAndFail(signs, checksPass, signs, checksFail ++ checkMin ++ checkMax)

      val singleTests = passSingle.map((_, 0)) ++ failSingle.map((_, 1))
      val multiTests = passMulti.map((_, 0)) ++ failMulti.map((_, 1))

      testSingleParameter("real_number_min_max", singleTests, "real_number_min_max_single")
      testSingleParameter("real_number_min_max_multiple", multiTests, "real_number_min_max_multi")
    }

  }

  test("Check whether double values with min and/or max specified are checked correctly using the awk fallback") {
    // change the tests so it won't find 'bc'
    val executableAwk = Paths.get(executable.toString + "_awk").toFile()
    Files.copy(executable.toPath(), executableAwk.toPath(), StandardCopyOption.REPLACE_EXISTING)
    Exec.run(
      Seq("sed", "-i'.original'", "s/command -v bc/command -v bcfoo/g", executableAwk.toString)
    )

    // min -3.2
    // max 5.7
    // traditionally bad arguments should already be checked elsewhere, so do less of them here
    val checkMin = Seq("-10", "-4", "-3.3")
    val checkMax = Seq("5.8", "6", "10")
    val checksPass = Seq("-3.2", "0", "2", "5.7")
    val checksFail = Seq("1+", "1-", "foo")
    val signs = Seq("")

    {
      val (passSingle, failSingle, passMulti, failMulti) = generatePassAndFail(signs, checksPass ++ checkMax, signs, checksFail ++ checkMin)

      val singleTests = passSingle.map((_, 0)) ++ failSingle.map((_, 1))
      val multiTests = passMulti.map((_, 0)) ++ failMulti.map((_, 1))

      testSingleParameter("real_number_min", singleTests, "real_number_min_single_awk")
      testSingleParameter("real_number_min_multiple", multiTests, "real_number_min_multi_awk")
    }

    {
      val (passSingle, failSingle, passMulti, failMulti) = generatePassAndFail(signs, checksPass ++ checkMin, signs, checksFail ++ checkMax)

      val singleTests = passSingle.map((_, 0)) ++ failSingle.map((_, 1))
      val multiTests = passMulti.map((_, 0)) ++ failMulti.map((_, 1))

      testSingleParameter("real_number_max", singleTests, "real_number_max_single_awk")
      testSingleParameter("real_number_max_multiple", multiTests, "real_number_max_multi_awk")
    }

    {
      val (passSingle, failSingle, passMulti, failMulti) = generatePassAndFail(signs, checksPass, signs, checksFail ++ checkMin ++ checkMax)

      val singleTests = passSingle.map((_, 0)) ++ failSingle.map((_, 1))
      val multiTests = passMulti.map((_, 0)) ++ failMulti.map((_, 1))

      testSingleParameter("real_number_min_max", singleTests, "real_number_min_max_single_awk")
      testSingleParameter("real_number_min_max_multiple", multiTests, "real_number_min_max_multi_awk")
    }

  }

  test("Check whether long values with min and/or max specified are checked correctly") {
    // min -3000000000
    // max 5000000000
    // traditionally bad arguments should already be checked elsewhere, so do less of them here
    val checkMin = Seq("-10000000000", "-3000000001")
    val checkMax = Seq("5000000001", "10000000000")
    val checksPass = Seq("-3000000000", "-2147483648", "2147483647", "5000000000")
    val checksFail = Seq("1.5", ".7", "1.5e5", "foo")
    val signs = Seq("")

    {
      val (passSingle, failSingle, passMulti, failMulti) = generatePassAndFail(signs, checksPass ++ checkMax, signs, checksFail ++ checkMin)

      val singleTests = passSingle.map((_, 0)) ++ failSingle.map((_, 1))
      val multiTests = passMulti.map((_, 0)) ++ failMulti.map((_, 1))

      testSingleParameter("long_number_min", singleTests, "long_number_min_single")
      testSingleParameter("long_number_min_multiple", multiTests, "long_number_min_multi")
    }

    {
      val (passSingle, failSingle, passMulti, failMulti) = generatePassAndFail(signs, checksPass ++ checkMin, signs, checksFail ++ checkMax)

      val singleTests = passSingle.map((_, 0)) ++ failSingle.map((_, 1))
      val multiTests = passMulti.map((_, 0)) ++ failMulti.map((_, 1))

      testSingleParameter("long_number_max", singleTests, "long_number_max_single")
      testSingleParameter("long_number_max_multiple", multiTests, "long_number_max_multi")
    }

    {
      val (passSingle, failSingle, passMulti, failMulti) = generatePassAndFail(signs, checksPass, signs, checksFail ++ checkMin ++ checkMax)

      val singleTests = passSingle.map((_, 0)) ++ failSingle.map((_, 1))
      val multiTests = passMulti.map((_, 0)) ++ failMulti.map((_, 1))

      testSingleParameter("long_number_min_max", singleTests, "long_number_min_max_single")
      testSingleParameter("long_number_min_max_multiple", multiTests, "long_number_min_max_multi")
    }

  }

  override def afterAll(): Unit = {
    IO.deleteRecursively(temporaryFolder)
  }
}