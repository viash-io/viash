package io.viash.auxiliary

import io.viash.{DockerTest, TestHelper}
import io.viash.config.Config
import io.viash.helpers.{IO, Exec, Logger}
import org.scalatest.BeforeAndAfterAll
import org.scalatest.funsuite.AnyFunSuite

import java.nio.file.{Files, Paths, StandardCopyOption}
import io.viash.ConfigDeriver
import io.viash.packageConfig.PackageConfig

class MainConfigViewNativeAuxiliaryResource extends AnyFunSuite with BeforeAndAfterAll {
  Logger.UseColorOverride.value = Some(false)
  private val temporaryFolder = IO.makeTemp("viash_tester")
  private val tempFolStr = temporaryFolder.toString


  private val configFile = getClass.getResource("/testbash/auxiliary_resource/config_resource_test.vsh.yaml").getPath
  private val dummyPackage = Some(PackageConfig(rootDir = Some(Paths.get(configFile).getParent())))
  private val sourceConfig = Config.read(configFile, viashPackage = dummyPackage)

  private val temporaryConfigFolder = IO.makeTemp(s"viash_${this.getClass.getName}_")
  private val configDeriver = ConfigDeriver(Paths.get(configFile), temporaryConfigFolder)

  test("Check resource paths are unchanged in config view") {

    // copy some resources to /tmp/viash_tmp_resources/ so we can test absolute path resources
    val tmpFolderResourceSourceFile = Paths.get(getClass.getResource("/testbash/auxiliary_resource/resource3.txt").getFile)

    val tmpFolderResourceDestinationFolder = Paths.get("/tmp/viash_tmp_resources/").toFile
    val tmpFolderResourceDestinationFile = Paths.get(tmpFolderResourceDestinationFolder.getPath, "resource3.txt")

    if (!tmpFolderResourceDestinationFolder.exists())
      tmpFolderResourceDestinationFolder.mkdir()

    Files.copy(tmpFolderResourceSourceFile, tmpFolderResourceDestinationFile, StandardCopyOption.REPLACE_EXISTING)

    // generate viash script
    val testOutput = TestHelper.testMain(
      workingDir = Some(temporaryConfigFolder),
      "config", "view",
      "--engine", "native",
      configFile
    )

    // Check the resources listed in the built .config.vsh.yaml file
    val builtConfigPath = temporaryFolder.resolve(".config.vsh.yaml")
    IO.write(testOutput.stdout, builtConfigPath)
    val dummyPackage = Some(PackageConfig(rootDir = Some(temporaryFolder)))
    val outputConfig = Config.read(builtConfigPath.toString, viashPackage = dummyPackage)

    val resourcePaths = outputConfig.resources.map(
      resource => {
        assert(resource.path.isDefined, s"Resource path is not defined for $resource")
        resource.path.get
      }
    )

    for (sourceResource <- sourceConfig.resources) {
      assert(resourcePaths.contains(sourceResource.path.get), s"Resource ${sourceResource.path.get} not found in built config")
    }

  }

  override def afterAll(): Unit = {
    IO.deleteRecursively(temporaryFolder)
    IO.deleteRecursively(temporaryConfigFolder)
  }
}