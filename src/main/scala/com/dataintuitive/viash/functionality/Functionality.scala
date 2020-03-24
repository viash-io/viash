package com.dataintuitive.viash.functionality

import scala.io.Source
import io.circe.yaml.parser
import java.nio.file.Paths
import java.io.File
import platforms.Platform

case class Functionality(
  name: String,
  description: Option[String],
  platform: Option[Platform], 
  inputs: List[DataObject[_]],
  outputs: List[DataObject[_]],
  resources: List[Resource]
) {
  val mainResource: Resource =
    resources.find(_.name.startsWith("main")).get
    
  require(
    resources.count(_.name.startsWith("main")) == 1,
    message = "Define exactly one resource whose name begins with 'main'."
  )
  
  require(
    platform.isDefined || (mainResource.path.isDefined && mainResource.isExecutable.getOrElse(true)),
    message = "If the platform is not specified, the main resource should be a standalone executable."
  )

  private var _rootDir: Option[File] = None
  
  def rootDir = {
    _rootDir match {
      case Some(f) => f
      case None => throw new RuntimeException("root directory of functionality object has not been defined yet")
    }
  }
  def rootDir_= (newValue: File) = {
    _rootDir = 
      if (newValue.isFile()) {
        Some(newValue.getParentFile())
      } else {
        Some(newValue)
      }
  }
    
  def mainCode: Option[String] = {
    if (platform.isEmpty) {
      None
    } else if (mainResource.code.isDefined) {
      Some(mainResource.code.get)
    } else {
      val mainPath = Paths.get(rootDir.getPath(), mainResource.path.get).toFile()
      Some(Source.fromFile(mainPath).mkString(""))
    }
  }
}

object Functionality {
  def parse(file: java.io.File): Functionality = {
    val str = Source.fromFile(file).mkString
    parser.parse(str)
      .fold(throw _, _.as[Functionality])
      .fold(throw _, identity)
  }
}