package io.viash

import java.io.File
import scala.sys.process._

object NextflowTestHelper {

  /**
    * Wrapper function for running Nextflow workflows
    *
    * @param mainScript Path to the main script
    * @param args Arguments for the workflow
    * @param cwd Working directory to execute command in
    * @param entry Entrypoint for the workflow
    * @param paramsFile Params file argument (if any)
    * @param extraEnv Extra environment variables to set
    * @param quiet Whether to suppress Nextflow meta information
    * @return Returns a Tuple3 containing the exit code, stdout and stderr.
    */
  def run(
    mainScript: String,
    args: List[String],
    cwd: File, 
    entry: Option[String] = None,
    paramsFile: Option[String] = None,
    extraEnv: Seq[(String, String)] = Nil,
    quiet: Boolean = false
  ): (Int, String, String) = {

    val stdOut = new StringBuilder
    val stdErr = new StringBuilder

    val command = 
      "nextflow" :: 
        { if (quiet) List("-q") else Nil } ::: 
        "run" :: "." ::
        "-main-script" :: mainScript ::
        { if (entry.isDefined) List("-entry", entry.get) else Nil } :::
        { if (paramsFile.isDefined) List("-params-file", paramsFile.get) else Nil } :::
        args

    val extraEnv_ = extraEnv :+ ( "NXF_VER" -> "22.04.5") // fix nextflow version to certain release

    val exitCode = Process(command, cwd, extraEnv_ : _*).!(ProcessLogger(str => stdOut ++= s"$str\n", str => stdErr ++= s"$str\n"))

    (exitCode, stdOut.toString, stdErr.toString)
  }
}