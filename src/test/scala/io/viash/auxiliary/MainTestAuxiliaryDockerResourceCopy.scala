package io.viash.auxiliary

import io.viash.{DockerTest, TestHelper}
import io.viash.helpers.{IO, Logger}
import org.scalatest.BeforeAndAfterAll
import org.scalatest.funsuite.AnyFunSuite

import java.nio.file.{Files, Paths, StandardCopyOption}
import scala.reflect.io.Directory
import io.viash.ConfigDeriver

class MainTestAuxiliaryDockerResourceCopy extends AnyFunSuite with BeforeAndAfterAll {
  Logger.UseColorOverride.value = Some(false)
  private val configFile = getClass.getResource("/testbash/auxiliary_resource/config_resource_test.vsh.yaml").getPath
  private val temporaryConfigFolder = IO.makeTemp(s"viash_${this.getClass.getName}_")
  private val configDeriver = ConfigDeriver(Paths.get(configFile), temporaryConfigFolder)

  test("Check resources are copied from and to the correct location", DockerTest) {

    // copy some resources to /tmp/viash_tmp_resources/ so we can test absolute path resources
    val tmpFolderResourceSourceFile = Paths.get(getClass.getResource("/testbash/resource3.txt").getFile)

    val tmpFolderResourceDestinationFolder = Paths.get("/tmp/viash_tmp_resources/").toFile
    val tmpFolderResourceDestinationFile = Paths.get(tmpFolderResourceDestinationFolder.getPath, "resource3.txt")

    if (!tmpFolderResourceDestinationFolder.exists())
      tmpFolderResourceDestinationFolder.mkdir()

    Files.copy(tmpFolderResourceSourceFile, tmpFolderResourceDestinationFile, StandardCopyOption.REPLACE_EXISTING)

    // generate viash script
    val testText = TestHelper.testMain(
      "test",
      "--engine", "docker",
      "-k", "true",
      configFile
    )

    // basic checks to see if standard test/build was correct
    assert(testText.contains("Running tests in temporary directory: "))
    assert(testText.contains("WARNING! No tests found!"))
    assert(!testText.contains("Cleaning up temporary directory"))

    val FolderRegex = ".*Running tests in temporary directory: '([^']*)'.*".r

    val tempPath = testText.replaceAll("\n", "") match {
      case FolderRegex(path) => path
      case _ => ""
    }

    // List all expected resources and their md5sum
    val expectedResources = List(
      //("check_bash_version.sh", "0c3c134d4ff0ea3a4a3b32e09fb7c100"),
      ("code.sh", ".*"), // turn off checksum match for code.sh, as it changes regularly.
      ("NOTICE", ".*"), // turn off checksum match for notice, as it changes regularly.
      ("resource1.txt", "bc9171172c4723589a247f99b838732d"),
      ("resource2.txt", "9cd530447200979dbf9e117915cbcc74"),
      ("resource_folder/resource_L1_1.txt", "51954bf10062451e683121e58d858417"),
      ("resource_folder/resource_L1_2.txt", "b43991c0ef5d15710faf976e02cbb206"),
      ("resource_folder/resource_L2/resource_L2_1.txt", "63165187f791a8dfff628ef8090e56ff"),
      ("target_folder/relocated_file_1.txt", "bc9171172c4723589a247f99b838732d"),
      ("target_folder/relocated_file_2.txt", "51954bf10062451e683121e58d858417"),
      ("target_folder/relocated_file_3.txt", ".*"), // turn off checksum match, as it changes regularly.
      ("resource3.txt", "aa2037b3d308bcb6a78a3d4fbf04b297"),
      ("target_folder/relocated_file_4.txt", "aa2037b3d308bcb6a78a3d4fbf04b297")
    )

    // Check all resources can be found in the folder
    for ((name, md5sum) <- expectedResources) {
      val resourceFile = Paths.get(tempPath, "build_engine_environment", name).toFile

      assert(resourceFile.exists, s"Could not find $name")

      val hash = TestHelper.computeHash(resourceFile.getPath)

      assert(md5sum.r.findFirstMatchIn(hash).isDefined, s"Calculated md5sum doesn't match the given md5sum for $name")
    }

    Directory(tmpFolderResourceDestinationFolder).deleteRecursively()
    checkTempDirAndRemove(testText, true, "viash_test_auxiliary_resources")
  }

  test("Check resources with unsupported format", DockerTest) {
    val configResourcesUnsupportedProtocolFile = configDeriver.derive(""".functionality.resources := [{type: "bash_script", path: "./check_bash_version.sh"}, {path: "ftp://ftp.ubuntu.com/releases/robots.txt"}]""", "config_resource_unsupported_protocol").toString
    // generate viash script
    val testOutput = TestHelper.testMainException2[RuntimeException](
      "test",
      "--engine", "docker",
      "-k", "true",
      configResourcesUnsupportedProtocolFile
    )

    assert(testOutput.exceptionText == "Unsupported scheme: ftp")

    // basic checks to see if standard test/build was correct
    assert(testOutput.output.contains("Running tests in temporary directory: "))
    assert(!testOutput.output.contains("WARNING! No tests found!"))
    assert(!testOutput.output.contains("Cleaning up temporary directory"))

    checkTempDirAndRemove(testOutput.output, true, "viash_test_auxiliary_resources")
  }

  /**
   * Searches the output generated by Main.main() during tests for the temporary directory name and verifies if it still exists or not.
   * If directory was expected to be present and actually is present, it will be removed.
   * @param testText the text generated by Main.main()
   * @param expectDirectoryExists expect the directory to be present or not
   * @return
   */
  def checkTempDirAndRemove(testText: String, expectDirectoryExists: Boolean, folderName: String = "viash_test_testbash"): Unit = {
    // Get temporary directory
    val FolderRegex = ".*Running tests in temporary directory: '([^']*)'.*".r

    val tempPath = testText.replaceAll("\n", "") match {
      case FolderRegex(path) => path
      case _ => ""
    }

    assert(tempPath.contains(s"${IO.tempDir}/$folderName"))

    val tempFolder = new Directory(Paths.get(tempPath).toFile)

    if (expectDirectoryExists) {
      // Check temporary directory is still present
      assert(tempFolder.exists)
      assert(tempFolder.isDirectory)

      // Remove the temporary directory
      tempFolder.deleteRecursively()
    }

    // folder should always have been removed at this stage
    assert(!tempFolder.exists)
  }

  override def afterAll(): Unit = {
    IO.deleteRecursively(temporaryConfigFolder)
  }
}
