package com.dataintuitive.viash

import java.io.FileWriter

import com.dataintuitive.viash.ViashTest.{ManyTestOutput, TestOutput}
import config.Config

object ViashNamespace {
  def build(
    configs: List[Config],
    target: String,
    setup: Boolean = false,
    parallel: Boolean = false
  ) {
    val configs2 = if (parallel) configs.par else configs

    configs2.foreach { conf =>
      val in = conf.info.get.parent_path
      val inTool = in.split("/").lastOption.getOrElse("WRONG")
      val platType = conf.platform.get.id
      val out =
        conf.functionality.namespace
          .map( ns => target + s"/$platType/$ns/$inTool").getOrElse(target + s"/$platType/$inTool")
      val namespaceOrNothing = conf.functionality.namespace.map( s => "(" + s + ")").getOrElse("")
      println(s"Exporting $in $namespaceOrNothing =$platType=> $out")
      ViashBuild(
        config = conf,
        output = out,
        namespace = conf.functionality.namespace,
        setup = setup
      )
    }
  }

  def test(
    configs: List[Config],
    parallel: Boolean = false,
    keepFiles: Option[Boolean] = None,
    tsv: Option[String] = None
  ): List[(Config, ManyTestOutput)] = {
    val configs2 = if (parallel) configs.par else configs

    // run all the component tests
    val tsvWriter = tsv.map(new FileWriter(_, false))

    try {
      tsvWriter.foreach(_.append(List("namespace", "functionality", "platform", "test_name", "exit_code", "result").mkString("\t") + sys.props("line.separator")))

      configs2.map { conf =>
        // get attributes
        val namespace = conf.functionality.namespace.getOrElse("")
        val funName = conf.functionality.name
        val platName = conf.platform.get.id

        // print start message
        printf(s"%s%20s %20s %20s %20s %8s %20s%s\n", "", namespace, funName, platName, "start", "", "", Console.RESET)

        // run tests
        // TODO: it would actually be great if this component could subscribe to testresults messages
        val ManyTestOutput(setupRes, testRes) = ViashTest(
          config = conf,
          keepFiles = keepFiles,
          quiet = true
        )
        val testResults =
          if (setupRes.exitValue > 0) {
            Nil
          } else if (testRes.isEmpty) {
            List(TestOutput("tests", -1, "no tests found", ""))
          } else {
            testRes
          }

        // print messages
        val results = setupRes :: testResults
        for (test ← results) {
          val (col, msg) = {
            if (test.exitValue > 0) {
              (Console.RED, "ERROR")
            } else if (test.exitValue < 0) {
              (Console.YELLOW, "MISSING")
            } else {
              (Console.GREEN, "SUCCESS")
            }
          }

          // print message
          printf(s"%s%20s %20s %20s %20s %8s %20s%s\n", col, namespace, funName, platName, test.name, test.exitValue, msg, Console.RESET)

          // write to tsv
          tsvWriter.foreach{writer =>
            writer.append(List(namespace, funName, platName, test.name, test.exitValue, msg).mkString("\t") + sys.props("line.separator"))
            writer.flush()
          }
        }

        // return output
        (conf, ManyTestOutput(setupRes, testRes))
      }.toList
    } finally {
      tsvWriter.foreach(_.close())
    }
  }

}
