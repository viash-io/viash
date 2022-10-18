package io.viash.auxiliary

import org.scalatest.BeforeAndAfterAll
import org.scalatest.funsuite.AnyFunSuite
import java.nio.file.{Paths, Files, StandardCopyOption}

import io.viash.config.Config

import scala.io.Source
import io.viash.helpers.{IO, Exec}
import io.viash.TestHelper

class MainBuildAuxiliaryNativeParameterCheck extends AnyFunSuite with BeforeAndAfterAll {
  // which platform to test
  private val configFile = getClass.getResource("/testbash/auxiliary_requirements/parameter_check.vsh.yaml").getPath

  private val temporaryFolder = IO.makeTemp("viash_tester")
  private val tempFolStr = temporaryFolder.toString

  // parse functionality from file
  private val functionality = Config.read(configFile, applyPlatform = false).functionality

  // check whether executable was created
  private val executable = Paths.get(tempFolStr, functionality.name).toFile

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
    ) yield v.mkString(":")
    val failCombined = for(
      c <- 1 until 3;
      v <- failSingles.combinations(c)
    ) yield v.mkString(":")
    val failMixCombined = for(
      a <- 1 until 3;
      b <- 1 until 3;
      p <- passSingles.take(4).combinations(a); // limit amount of tests with .take, this becomes big *fast*
      f <- failSingles.take(4).combinations(b); // limit amount of tests with .take, this becomes big *fast*
      order <- Seq(true, false)
    ) yield if (order) (p++f).mkString(":") else (f++p).mkString(":")
    (passSingles, failSingles, passCombined, failCombined++failMixCombined)
  }

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

    val (passSingle, failSingle, passMulti, failMulti) = generatePassAndFail(signs, checksPass, signs, checksFail)

    val tests = Seq(
      ("--real_number", passSingle, 0),
      ("--real_number", failSingle, 1),
      ("--real_number_multiple", passMulti, 0),
      ("--real_number_multiple", failMulti, 1)
    )

    for (
      (param, tests, expected_result) <- tests;
      value <- tests
    ) {
      val out = Exec.runCatch(
        Seq(
          executable.toString,
          param, value,
        )
      )
      assert(out.exitValue == expected_result, s"Test failed for $param: $value\n${out.output}")
    }

  }

  test("Check whether integer values are checked correctly") {
    // Decision was made to allow quite permissive syntax checking
    val checksPass = Seq("0", "1", "456")
    val checksFail = Seq("1.5", ".7", "1.5e5", "2.6e+6", "3.8e-7", "1+", "1-", "1+2", "1-2", "foo", "123foo", "123.foo", "12e0.5")
    val signs = Seq("", "+", "-")

    val (passSingle, failSingle, passMulti, failMulti) = generatePassAndFail(signs, checksPass, signs, checksFail)

    val tests = Seq(
      ("--whole_number", passSingle, 0),
      ("--whole_number", failSingle, 1),
      ("--whole_number_multiple", passMulti, 0),
      ("--whole_number_multiple", failMulti, 1)
    )

    for (
      (param, tests, expected_result) <- tests;
      value <- tests
    ) {
      val out = Exec.runCatch(
        Seq(
          executable.toString,
          param, value,
        )
      )
      assert(out.exitValue == expected_result, s"Test failed for $param: $value\n${out.output}")
    }

  }

  test("Check whether boolean values are checked correctly") {
    // Decision was made to allow quite permissive syntax checking
    val checksPass = Seq("true", "True", "TRUE", "false", "False", "FALSE", "yes", "Yes", "YES", "no", "No", "NO")
    val checksFail = Seq("0", "1", "foo", "tRuE", "fALSE")
    val signs = Seq("", "+", "-")

    val (passSingle, failSingle, passMulti, failMulti) = generatePassAndFail(Seq(""), checksPass, signs, checksFail)

    val tests = Seq(
      ("--reality", passSingle, 0),
      ("--reality", failSingle, 1),
      ("--reality_multiple", passMulti, 0),
      ("--reality_multiple", failMulti, 1)
    )

    for (
      (param, tests, expected_result) <- tests;
      value <- tests
    ) {
      val out = Exec.runCatch(
        Seq(
          executable.toString,
          param, value,
        )
      )
      assert(out.exitValue == expected_result, s"Test failed for $param: $value\n${out.output}")
    }

    // Signs are never allowed, ie. "+true", "-false", etc
    for(
      value <- checksPass;
      sign <- Seq("+", "-")
    ) {
      val param = sign + value
      val out = Exec.runCatch(
        Seq(
          executable.toString,
          "--reality", param,
        )
      )
      assert(out.exitValue != 0, s"Test reality: $param should fail\n${out.output}")
    }

  }

  test("Check whether string values with allowed values are checked correctly") {
    // Decision was made to allow quite permissive syntax checking
    val checksPass = Seq("one", "2", "two words", "three separate words", 
      "Two full blown sentences with punctuation. The weather is nice today!", "Capital",
      "ALL CAPITALS", "a", "B")
    val checksFail = Seq("0", "1", "foo", "bone", "one 2")

    val (passSingle, failSingle, passMulti, failMulti) = generatePassAndFail(Seq(""), checksPass, Seq(""), checksFail)

    val tests = Seq(
      ("--string", passSingle, 0),
      ("--string", failSingle, 1),
      ("--multiple", passMulti, 0),
      ("--multiple", failMulti, 1)
    )

    for (
      (param, tests, expected_result) <- tests;
      value <- tests
    ) {
      val out = Exec.runCatch(
        Seq(
          executable.toString,
          param, value,
        )
      )
      assert(out.exitValue == expected_result, s"Test failed for $param: $value\n${out.output}")
    }

  }

  test("Check whether integer values with allowed values are checked correctly") {
    val checksPass = Seq("0", "1", "3", "-10")
    val checksFail = Seq("2", "-1", "foo")

    val (passSingle, failSingle, passMulti, failMulti) = generatePassAndFail(Seq(""), checksPass, Seq(""), checksFail)

    val tests = Seq(
      ("--whole_number_choice", passSingle, 0),
      ("--whole_number_choice", failSingle, 1),
      ("--whole_number_choice_multiple", passMulti, 0),
      ("--whole_number_choice_multiple", failMulti, 1)
    )

    for (
      (param, tests, expected_result) <- tests;
      value <- tests
    ) {
      val out = Exec.runCatch(
        Seq(
          executable.toString,
          param, value,
        )
      )
      assert(out.exitValue == expected_result, s"Test failed for $param: $value\n${out.output}")
    }

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

      val tests = Seq(
        ("--whole_number_min", passSingle, 0),
        ("--whole_number_min", failSingle, 1),
        ("--whole_number_min_multiple", passMulti, 0),
        ("--whole_number_min_multiple", failMulti, 1)
      )

      for (
        (param, tests, expected_result) <- tests;
        value <- tests
      ) {
        val out = Exec.runCatch(
          Seq(
            executable.toString,
            param, value,
          )
        )
        assert(out.exitValue == expected_result, s"Test failed for min $param: $value\n${out.output}")
      }
    }

    {
      val (passSingle, failSingle, passMulti, failMulti) = generatePassAndFail(signs, checksPass ++ checkMin, signs, checksFail ++ checkMax)

      val tests = Seq(
        ("--whole_number_max", passSingle, 0),
        ("--whole_number_max", failSingle, 1),
        ("--whole_number_max_multiple", passMulti, 0),
        ("--whole_number_max_multiple", failMulti, 1)
      )

      for (
        (param, tests, expected_result) <- tests;
        value <- tests
      ) {
        val out = Exec.runCatch(
          Seq(
            executable.toString,
            param, value,
          )
        )
        assert(out.exitValue == expected_result, s"Test failed for max $param: $value\n${out.output}")
      }
    }

    {
      val (passSingle, failSingle, passMulti, failMulti) = generatePassAndFail(signs, checksPass, signs, checksFail ++ checkMin ++ checkMax)

      val tests = Seq(
        ("--whole_number_min_max", passSingle, 0),
        ("--whole_number_min_max", failSingle, 1),
        ("--whole_number_min_max_multiple", passMulti, 0),
        ("--whole_number_min_max_multiple", failMulti, 1)
      )

      for (
        (param, tests, expected_result) <- tests;
        value <- tests
      ) {
        val out = Exec.runCatch(
          Seq(
            executable.toString,
            param, value,
          )
        )
        assert(out.exitValue == expected_result, s"Test failed for minmax $param: $value\n${out.output}")
      }
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

      val tests = Seq(
        ("--real_number_min", passSingle, 0),
        ("--real_number_min", failSingle, 1),
        ("--real_number_min_multiple", passMulti, 0),
        ("--real_number_min_multiple", failMulti, 1)
      )

      for (
        (param, tests, expected_result) <- tests;
        value <- tests
      ) {
        val out = Exec.runCatch(
          Seq(
            executable.toString,
            param, value,
          )
        )
        assert(out.exitValue == expected_result, s"Test failed for min $param: $value\n${out.output}")
      }
    }

    {
      val (passSingle, failSingle, passMulti, failMulti) = generatePassAndFail(signs, checksPass ++ checkMin, signs, checksFail ++ checkMax)

      val tests = Seq(
        ("--real_number_max", passSingle, 0),
        ("--real_number_max", failSingle, 1),
        ("--real_number_max_multiple", passMulti, 0),
        ("--real_number_max_multiple", failMulti, 1)
      )

      for (
        (param, tests, expected_result) <- tests;
        value <- tests
      ) {
        val out = Exec.runCatch(
          Seq(
            executable.toString,
            param, value,
          )
        )
        assert(out.exitValue == expected_result, s"Test failed for max $param: $value\n${out.output}")
      }
    }

    {
      val (passSingle, failSingle, passMulti, failMulti) = generatePassAndFail(signs, checksPass, signs, checksFail ++ checkMin ++ checkMax)

      val tests = Seq(
        ("--real_number_min_max", passSingle, 0),
        ("--real_number_min_max", failSingle, 1),
        ("--real_number_min_max_multiple", passMulti, 0),
        ("--real_number_min_max_multiple", failMulti, 1)
      )

      for (
        (param, tests, expected_result) <- tests;
        value <- tests
      ) {
        val out = Exec.runCatch(
          Seq(
            executable.toString,
            param, value,
          )
        )
        assert(out.exitValue == expected_result, s"Test failed for minmax $param: $value\n${out.output}")
      }
    }

  }

  test("Check whether double values with min and/or max specified are checked correctly using the awk fallback") {
    // change the tests so it won't find 'bc'
    val executableAwk = Paths.get(executable + "_awk").toFile()
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

      val tests = Seq(
        ("--real_number_min", passSingle, 0),
        ("--real_number_min", failSingle, 1),
        ("--real_number_min_multiple", passMulti, 0),
        ("--real_number_min_multiple", failMulti, 1)
      )

      for (
        (param, tests, expected_result) <- tests;
        value <- tests
      ) {
        val out = Exec.runCatch(
          Seq(
            executable.toString,
            param, value,
          )
        )
        assert(out.exitValue == expected_result, s"Test failed for min $param: $value\n${out.output}")
      }
    }

    {
      val (passSingle, failSingle, passMulti, failMulti) = generatePassAndFail(signs, checksPass ++ checkMin, signs, checksFail ++ checkMax)

      val tests = Seq(
        ("--real_number_max", passSingle, 0),
        ("--real_number_max", failSingle, 1),
        ("--real_number_max_multiple", passMulti, 0),
        ("--real_number_max_multiple", failMulti, 1)
      )

      for (
        (param, tests, expected_result) <- tests;
        value <- tests
      ) {
        val out = Exec.runCatch(
          Seq(
            executable.toString,
            param, value,
          )
        )
        assert(out.exitValue == expected_result, s"Test failed for max $param: $value\n${out.output}")
      }
    }

    {
      val (passSingle, failSingle, passMulti, failMulti) = generatePassAndFail(signs, checksPass, signs, checksFail ++ checkMin ++ checkMax)

      val tests = Seq(
        ("--real_number_min_max", passSingle, 0),
        ("--real_number_min_max", failSingle, 1),
        ("--real_number_min_max_multiple", passMulti, 0),
        ("--real_number_min_max_multiple", failMulti, 1)
      )

      for (
        (param, tests, expected_result) <- tests;
        value <- tests
      ) {
        val out = Exec.runCatch(
          Seq(
            executable.toString,
            param, value,
          )
        )
        assert(out.exitValue == expected_result, s"Test failed for minmax $param: $value\n${out.output}")
      }
    }

  }

  override def afterAll() {
    IO.deleteRecursively(temporaryFolder)
  }
}