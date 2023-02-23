package io.viash.helpers

import org.scalatest.BeforeAndAfterAll
import org.scalatest.funsuite.AnyFunSuite
import java.nio.file.{Files, Paths, StandardCopyOption}
import java.nio.file.Path
import scala.collection.mutable.ListBuffer

class GitTest extends AnyFunSuite with BeforeAndAfterAll {
  val fakeGitRepo = "git@non.existing.repo:viash/meta-test"

  val tempPaths = ListBuffer[Path]()
  def makeTemp(name: String): Path = {
    val tempPath = IO.makeTemp(name)
    tempPaths += tempPath
    tempPath
  }

  test("Check git metadata of empty dir") {
    val tempDir = makeTemp("viash_test_meta_1_").toFile
    assert(!Git.isGitRepo(tempDir), "Git.isGitRepo")
    assert(Git.getCommit(tempDir).isEmpty, "Git.getCommit")
    assert(Git.getLocalRepo(tempDir).isEmpty, "Git.getLocalRepo")
    assert(Git.getRemoteRepo(tempDir).isEmpty, "Git.getRemoteRepo")
    assert(Git.getTag(tempDir).isEmpty, "Git.getTag")
  }

  test("Check git metadata after git init") {
    val tempDir = makeTemp("viash_test_meta_2_").toFile

    val gitInitOut = Exec.runCatch(List("git", "init"), cwd = Some(tempDir))
    assert(gitInitOut.exitValue == 0, s"git init: ${gitInitOut.output}")

    val gitInfo = Git.getInfo(tempDir)
    assert(Git.isGitRepo(tempDir), "Git.isGitRepo")
    assert(Git.getCommit(tempDir).isEmpty, "Git.getCommit")
    val lr = Git.getLocalRepo(tempDir)
    assert(lr.isDefined && lr.get.contains(tempDir.toString), "Git.getLocalRepo")
    assert(Git.getRemoteRepo(tempDir).isEmpty, "Git.getRemoteRepo")
    assert(Git.getTag(tempDir).isEmpty, "Git.getTag")
  }

  test("Check git metadata after git remote add") {
    val tempDir = makeTemp("viash_test_meta_3_").toFile

    val gitInitOut = Exec.runCatch(List("git", "init"), cwd = Some(tempDir))
    assert(gitInitOut.exitValue == 0, s"git init: ${gitInitOut.output}")

    val gitRemoteAddOut = Exec.runCatch(List("git", "remote", "add", "origin", fakeGitRepo), cwd = Some(tempDir))
    assert(gitRemoteAddOut.exitValue == 0, s"git remote add: ${gitRemoteAddOut.output}")

    val gitInfo = Git.getInfo(tempDir)
    assert(Git.isGitRepo(tempDir), "Git.isGitRepo")
    assert(Git.getCommit(tempDir).isEmpty, "Git.getCommit")
    val lr = Git.getLocalRepo(tempDir)
    assert(lr.isDefined && lr.get.contains(tempDir.toString), "Git.getLocalRepo")
    assert(Git.getRemoteRepo(tempDir) == Some(fakeGitRepo), "Git.getRemoteRepo")
    assert(Git.getTag(tempDir).isEmpty, "Git.getTag")
  }

  test("Check git metadata after git remote add, but remote definition contains credentials username") {
    val fakeGitRepo = "https://foobar@github.com/viash/meta-test.git"
    val tempDir = makeTemp("viash_test_meta_4_").toFile

    val gitInitOut = Exec.runCatch(List("git", "init"), cwd = Some(tempDir))
    assert(gitInitOut.exitValue == 0, s"git init: ${gitInitOut.output}")

    val gitRemoteAddOut = Exec.runCatch(List("git", "remote", "add", "origin", fakeGitRepo), cwd = Some(tempDir))
    assert(gitRemoteAddOut.exitValue == 0, s"git remote add: ${gitRemoteAddOut.output}")

    val gitInfo = Git.getInfo(tempDir)
    assert(Git.isGitRepo(tempDir), "Git.isGitRepo")
    assert(Git.getCommit(tempDir).isEmpty, "Git.getCommit")
    val lr = Git.getLocalRepo(tempDir)
    assert(lr.isDefined && lr.get.contains(tempDir.toString), "Git.getLocalRepo")
    assert(Git.getRemoteRepo(tempDir) == Some("https://github.com/viash/meta-test.git"), "Git.getRemoteRepo")
    assert(Git.getTag(tempDir).isEmpty, "Git.getTag")
  }

  test("Check git metadata after git remote add, but remote definition contains credentials username & password/PAT") {
    val fakeGitRepo = "https://foobar:ghp_SGFoLCB0aGlzIGlzIG5vdCBhIHJlYWwgUEFU@github.com/viash/meta-test.git"
    val tempDir = makeTemp("viash_test_meta_5_").toFile

    val gitInitOut = Exec.runCatch(List("git", "init"), cwd = Some(tempDir))
    assert(gitInitOut.exitValue == 0, s"git init: ${gitInitOut.output}")

    val gitRemoteAddOut = Exec.runCatch(List("git", "remote", "add", "origin", fakeGitRepo), cwd = Some(tempDir))
    assert(gitRemoteAddOut.exitValue == 0, s"git remote add: ${gitRemoteAddOut.output}")

    val gitInfo = Git.getInfo(tempDir)
    assert(Git.isGitRepo(tempDir), "Git.isGitRepo")
    assert(Git.getCommit(tempDir).isEmpty, "Git.getCommit")
    val lr = Git.getLocalRepo(tempDir)
    assert(lr.isDefined && lr.get.contains(tempDir.toString), "Git.getLocalRepo")
    assert(Git.getRemoteRepo(tempDir) == Some("https://github.com/viash/meta-test.git"), "Git.getRemoteRepo")
    assert(Git.getTag(tempDir).isEmpty, "Git.getTag")
  }

  test("Check git metadata after git commit") {
    val tempDir = makeTemp("viash_test_meta_6_").toFile

    val gitInitOut = Exec.runCatch(List("git", "init"), cwd = Some(tempDir))
    assert(gitInitOut.exitValue == 0, s"git init: ${gitInitOut.output}")

    val gitRemoteAddOut = Exec.runCatch(List("git", "remote", "add", "origin", fakeGitRepo), cwd = Some(tempDir))
    assert(gitRemoteAddOut.exitValue == 0, s"git remote add: ${gitRemoteAddOut.output}")

    val tempFile = tempDir.toPath.resolve("file.txt")
    Files.write(tempFile, "Foo".getBytes())

    val gitAddOut = Exec.runCatch(List("git", "add", "file.txt"), cwd = Some(tempDir))
    assert(gitAddOut.exitValue == 0, s"git add: ${gitAddOut.output}")

    val gitConfigName = Exec.runCatch(List("git", "config", "user.name", "Viash CI"), cwd = Some(tempDir))
    assert(gitConfigName.exitValue == 0, s"git config name: ${gitConfigName.output}")

    val gitConfigEmail = Exec.runCatch(List("git", "config", "user.email", "viash_test_build@viash.io"), cwd = Some(tempDir))
    assert(gitConfigEmail.exitValue == 0, s"git config email: ${gitConfigEmail.output}")

    val gitCommitOut = Exec.runCatch(List("git", "commit", "-m", "initial commit"), cwd = Some(tempDir))
    assert(gitCommitOut.exitValue == 0, s"git commit: ${gitCommitOut.output}")

    val gitInfo = Git.getInfo(tempDir)
    assert(Git.isGitRepo(tempDir), "Git.isGitRepo")
    assert(Git.getCommit(tempDir).isDefined, "Git.getCommit")
    val lr = Git.getLocalRepo(tempDir)
    assert(lr.isDefined && lr.get.contains(tempDir.toString), "Git.getLocalRepo")
    assert(Git.getRemoteRepo(tempDir) == Some(fakeGitRepo), "Git.getRemoteRepo")
    assert(Git.getTag(tempDir).isEmpty, "Git.getTag")
  }

  test("Check git metadata after git tag") {
    val tempDir = makeTemp("viash_test_meta_7_").toFile

    val gitInitOut = Exec.runCatch(List("git", "init"), cwd = Some(tempDir))
    assert(gitInitOut.exitValue == 0, s"git init: ${gitInitOut.output}")

    val gitRemoteAddOut = Exec.runCatch(List("git", "remote", "add", "origin", fakeGitRepo), cwd = Some(tempDir))
    assert(gitRemoteAddOut.exitValue == 0, s"git remote add: ${gitRemoteAddOut.output}")

    val tempFile = tempDir.toPath.resolve("file.txt")
    Files.write(tempFile, "Foo".getBytes())

    val gitAddOut = Exec.runCatch(List("git", "add", "file.txt"), cwd = Some(tempDir))
    assert(gitAddOut.exitValue == 0, s"git add: ${gitAddOut.output}")

    val gitConfigName = Exec.runCatch(List("git", "config", "user.name", "Viash CI"), cwd = Some(tempDir))
    assert(gitConfigName.exitValue == 0, s"git config name: ${gitConfigName.output}")
    
    val gitConfigEmail = Exec.runCatch(List("git", "config", "user.email", "viash_test_build@viash.io"), cwd = Some(tempDir))
    assert(gitConfigEmail.exitValue == 0, s"git config email: ${gitConfigEmail.output}")

    val gitCommitOut = Exec.runCatch(List("git", "commit", "-m", "initial commit"), cwd = Some(tempDir))
    assert(gitCommitOut.exitValue == 0, s"git commit: ${gitCommitOut.output}")

    val gitTagOut = Exec.runCatch(List("git", "tag", "-a", "0.1.1", "-m", "first tag"), cwd = Some(tempDir))
    assert(gitTagOut.exitValue == 0, s"git tag: ${gitTagOut.output}")

    val gitInfo = Git.getInfo(tempDir)
    assert(Git.isGitRepo(tempDir), "Git.isGitRepo")
    assert(Git.getCommit(tempDir).isDefined, "Git.getCommit")
    val lr = Git.getLocalRepo(tempDir)
    assert(lr.isDefined && lr.get.contains(tempDir.toString), "Git.getLocalRepo")
    assert(Git.getRemoteRepo(tempDir) == Some(fakeGitRepo), "Git.getRemoteRepo")
    assert(Git.getTag(tempDir) == Some("0.1.1"), "Git.getTag")
  }

  override def afterAll(): Unit = {
    tempPaths.foreach(tempDir => IO.deleteRecursively(tempDir))
  }
}