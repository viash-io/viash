package io.viash.auxiliary

import io.viash.{DockerTest, TestHelper}
import io.viash.config.Config
import io.viash.helpers.{IO, Exec, Logger}
import org.scalatest.BeforeAndAfterAll
import org.scalatest.funsuite.AnyFunSuite

import java.nio.file.{Files, Paths, StandardCopyOption}
import io.viash.ConfigDeriver

class MainBuildAuxiliaryDockerResourceCopying extends AnyFunSuite with BeforeAndAfterAll {
  Logger.UseColorOverride.value = Some(false)
  private val temporaryFolder = IO.makeTemp("viash_tester")
  private val tempFolStr = temporaryFolder.toString


  private val configFile = getClass.getResource("/testbash/auxiliary_resource/config_resource_test.vsh.yaml").getPath
  private val functionality = Config.read(configFile).functionality
  private val executable = Paths.get(tempFolStr, functionality.name).toFile

  private val temporaryConfigFolder = IO.makeTemp(s"viash_${this.getClass.getName}_")
  private val configDeriver = ConfigDeriver(Paths.get(configFile), temporaryConfigFolder)

  test("Check resources are copied from and to the correct location") {

    // copy some resources to /tmp/viash_tmp_resources/ so we can test absolute path resources
    val tmpFolderResourceSourceFile = Paths.get(getClass.getResource("/testbash/auxiliary_resource/resource3.txt").getFile)

    val tmpFolderResourceDestinationFolder = Paths.get("/tmp/viash_tmp_resources/").toFile
    val tmpFolderResourceDestinationFile = Paths.get(tmpFolderResourceDestinationFolder.getPath, "resource3.txt")

    if (!tmpFolderResourceDestinationFolder.exists())
      tmpFolderResourceDestinationFolder.mkdir()

    Files.copy(tmpFolderResourceSourceFile, tmpFolderResourceDestinationFile, StandardCopyOption.REPLACE_EXISTING)

    // generate viash script
    TestHelper.testMain(
      "build",
      "--engine", "docker",
      "-o", tempFolStr,
      configFile
    )

    assert(executable.exists)
    assert(executable.canExecute)

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
      ("target_folder/relocated_file_3.txt", ".*"), // turn off checksum match
      ("resource3.txt", "aa2037b3d308bcb6a78a3d4fbf04b297"),
      ("target_folder/relocated_file_4.txt", "aa2037b3d308bcb6a78a3d4fbf04b297")
    )

    // Check all resources can be found in the folder
    for ((name, md5sum) <- expectedResources) {
      val resourceFile = Paths.get(tempFolStr, name).toFile

      assert(resourceFile.exists, s"Could not find $name")

      val hash = TestHelper.computeHash(resourceFile.getPath)
      assert(md5sum.r.findFirstMatchIn(hash).isDefined, s"Calculated md5sum doesn't match the given md5sum for $resourceFile")
    }
  }

  test("Check resources with unsupported format") {
    val configResourcesUnsupportedProtocolFile = configDeriver.derive(""".functionality.resources := [{type: "bash_script", path: "./check_bash_version.sh"}, {path: "ftp://ftp.ubuntu.com/releases/robots.txt"}]""", "config_resource_unsupported_protocol").toString
    // generate viash script
    val testOutput = TestHelper.testMainException2[RuntimeException](
      "build",
      "--engine", "docker",
      "-o", tempFolStr,
      configResourcesUnsupportedProtocolFile
    )

    assert(testOutput.exceptionText == "Unsupported scheme: ftp")
  }

  override def afterAll(): Unit = {
    IO.deleteRecursively(temporaryFolder)
    IO.deleteRecursively(temporaryConfigFolder)
  }
}