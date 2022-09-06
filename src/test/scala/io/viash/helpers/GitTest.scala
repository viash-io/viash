package io.viash.helpers

import org.scalatest.{BeforeAndAfterAll, FunSuite}
import java.nio.file.{Files, Paths, StandardCopyOption}

class GitTest extends FunSuite with BeforeAndAfterAll {
  val fakeGitRepo = "git@non.existing.repo:viash/meta-test"

  test("Check git metadata of empty dir") {
    val tempDir = IO.makeTemp("viash_test_meta_1_").toFile
    assert(!Git.isGitRepo(tempDir), "Git.isGitRepo")
    assert(Git.getCommit(tempDir).isEmpty, "Git.getCommit")
    assert(Git.getLocalRepo(tempDir).isEmpty, "Git.getLocalRepo")
    assert(Git.getRemoteRepo(tempDir).isEmpty, "Git.getRemoteRepo")
    assert(Git.getTag(tempDir).isEmpty, "Git.getTag")
  }

  test("Check git metadata after git init") {
    val tempDir = IO.makeTemp("viash_test_meta_2_").toFile

    val gitInitOut = Exec.runOpt(List("git", "init"), cwd = Some(tempDir))
    assert(gitInitOut.isDefined, "git init")

    val gitInfo = Git.getInfo(tempDir)
    assert(Git.isGitRepo(tempDir), "Git.isGitRepo")
    assert(Git.getCommit(tempDir).isEmpty, "Git.getCommit")
    val lr = Git.getLocalRepo(tempDir)
    assert(lr.isDefined && lr.get.contains(tempDir.toString), "Git.getLocalRepo")
    assert(Git.getRemoteRepo(tempDir).isEmpty, "Git.getRemoteRepo")
    assert(Git.getTag(tempDir).isEmpty, "Git.getTag")
  }

  test("Check git metadata after git remote add") {
    val tempDir = IO.makeTemp("viash_test_meta_3_").toFile

    val gitInitOut = Exec.runOpt(List("git", "init"), cwd = Some(tempDir))
    assert(gitInitOut.isDefined, "git init")

    val gitRemoteAddOut = Exec.runOpt(List("git", "remote", "add", "origin", fakeGitRepo), cwd = Some(tempDir))
    assert(gitRemoteAddOut.isDefined, "git remote add")

    val gitInfo = Git.getInfo(tempDir)
    assert(Git.isGitRepo(tempDir), "Git.isGitRepo")
    assert(Git.getCommit(tempDir).isEmpty, "Git.getCommit")
    val lr = Git.getLocalRepo(tempDir)
    assert(lr.isDefined && lr.get.contains(tempDir.toString), "Git.getLocalRepo")
    assert(Git.getRemoteRepo(tempDir) == Some(fakeGitRepo), "Git.getRemoteRepo")
    assert(Git.getTag(tempDir).isEmpty, "Git.getTag")
  }

  test("Check git metadata after git commit") {
    val tempDir = IO.makeTemp("viash_test_meta_4_").toFile

    val gitInitOut = Exec.runOpt(List("git", "init"), cwd = Some(tempDir))
    assert(gitInitOut.isDefined, "git init")

    val gitRemoteAddOut = Exec.runOpt(List("git", "remote", "add", "origin", fakeGitRepo), cwd = Some(tempDir))
    assert(gitRemoteAddOut.isDefined, "git remote add")

    val tempFile = tempDir.toPath.resolve("file.txt")
    Files.write(tempFile, "Foo".getBytes())

    val gitAddOut = Exec.runOpt(List("git", "add", "file.txt"), cwd = Some(tempDir))
    assert(gitAddOut.isDefined, "git add")

    val gitCommitOut = Exec.runOpt(List("git", "commit", "-m", "initial commit"), cwd = Some(tempDir))
    assert(gitCommitOut.isDefined, "git commit")

    val gitInfo = Git.getInfo(tempDir)
    assert(Git.isGitRepo(tempDir), "Git.isGitRepo")
    assert(Git.getCommit(tempDir).isDefined, "Git.getCommit")
    val lr = Git.getLocalRepo(tempDir)
    assert(lr.isDefined && lr.get.contains(tempDir.toString), "Git.getLocalRepo")
    assert(Git.getRemoteRepo(tempDir) == Some(fakeGitRepo), "Git.getRemoteRepo")
    assert(Git.getTag(tempDir).isEmpty, "Git.getTag")
  }

  test("Check git metadata after git tag") {
    val tempDir = IO.makeTemp("viash_test_meta_5_").toFile

    val gitInitOut = Exec.runOpt(List("git", "init"), cwd = Some(tempDir))
    assert(gitInitOut.isDefined, "git init")

    val gitRemoteAddOut = Exec.runOpt(List("git", "remote", "add", "origin", fakeGitRepo), cwd = Some(tempDir))
    assert(gitRemoteAddOut.isDefined, "git remote add")

    val tempFile = tempDir.toPath.resolve("file.txt")
    Files.write(tempFile, "Foo".getBytes())

    val gitAddOut = Exec.runOpt(List("git", "add", "file.txt"), cwd = Some(tempDir))
    assert(gitAddOut.isDefined, "git add")

    val gitCommitOut = Exec.runOpt(List("git", "commit", "-m", "initial commit"), cwd = Some(tempDir))
    assert(gitCommitOut.isDefined, "git commit")

    val gitTagOut = Exec.runOpt(List("git", "tag", "-a", "0.1.1", "-m", "first tag"), cwd = Some(tempDir))
    assert(gitTagOut.isDefined, "git tag")

    val gitInfo = Git.getInfo(tempDir)
    assert(Git.isGitRepo(tempDir), "Git.isGitRepo")
    assert(Git.getCommit(tempDir).isDefined, "Git.getCommit")
    val lr = Git.getLocalRepo(tempDir)
    assert(lr.isDefined && lr.get.contains(tempDir.toString), "Git.getLocalRepo")
    assert(Git.getRemoteRepo(tempDir) == Some(fakeGitRepo), "Git.getRemoteRepo")
    assert(Git.getTag(tempDir) == Some("0.1.1"), "Git.getTag")
  }
}