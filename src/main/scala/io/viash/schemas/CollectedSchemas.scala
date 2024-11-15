/*
 * Copyright (C) 2020  Data Intuitive
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package io.viash.schemas

import io.circe.{Encoder, Printer => JsonPrinter}
import io.circe.syntax.EncoderOps

import io.viash.functionality._
import io.viash.runners._
import io.viash.engines._
import io.viash.platforms._
import io.circe.Json
import io.viash.config.Config
import io.viash.config.BuildInfo
import io.viash.packageConfig.PackageConfig
import io.viash.helpers._
import io.viash.runners.nextflow.{NextflowConfig, NextflowAuto, NextflowDirectives}
import io.viash.engines.requirements._
import io.viash.config.arguments._
import io.viash.config.dependencies._
import io.viash.config.resources._
import io.viash.config.ArgumentGroup
import io.viash.config.Author
import io.viash.config.ComputationalRequirements
import io.viash.config.Links
import io.viash.config.References
import scala.compiletime.{ codeOf, constValue, erasedValue, error, summonFrom, summonInline }
import scala.annotation.Annotation

object CollectedSchemas {
  private val jsonPrinter = JsonPrinter.spaces2.copy(dropNullValues = true)

  import io.viash.helpers.circe._

  private implicit val encodeParameterSchema: Encoder.AsObject[ParameterSchema] = deriveConfiguredEncoderStrict
  private implicit val encodeDeprecatedOrRemoved: Encoder.AsObject[DeprecatedOrRemovedSchema] = deriveConfiguredEncoder
  private implicit val encodeExample: Encoder.AsObject[ExampleSchema] = deriveConfiguredEncoder

  private inline def getMembers[T](): List[ParameterSchema] = {
    val tpe = typeOf[T]
    val history = historyOf[T]
    val annotations = annotationsOf[T]
    val thisMembers = ParameterSchema("__this__", tpe, history, annotations)

    val memberAnnotations = memberTypeAnnotationsOf[T].map({ case (memberName, memberType, memberAnns) => 
      ParameterSchema(memberName, memberType, Nil, memberAnns)
    })
    thisMembers +: memberAnnotations
  }

  // split the data in two parts to avoid the compiler complaining about the size
  object memberData_part1 {
    val part = List(
      getMembers[Config](),
      getMembers[PackageConfig](),
      getMembers[BuildInfo](),
      getMembers[SysEnvTrait](),

      getMembers[Functionality](),
      getMembers[Author](),
      getMembers[ComputationalRequirements](),
      getMembers[ArgumentGroup](),
      getMembers[Links](),
      getMembers[References](),

      getMembers[Runner](),
      getMembers[ExecutableRunner](),
      getMembers[NextflowRunner](),

      getMembers[Engine](),
      getMembers[NativeEngine](),
      getMembers[DockerEngine](),

      getMembers[Platform](),
      getMembers[NativePlatform](),
      getMembers[DockerPlatform](),
      getMembers[NextflowPlatform](),

      getMembers[Requirements](),
      getMembers[ApkRequirements](),
      getMembers[AptRequirements](),
      getMembers[DockerRequirements](),
      getMembers[JavaScriptRequirements](),
      getMembers[PythonRequirements](),
      getMembers[RRequirements](),
      getMembers[RubyRequirements](),
      getMembers[YumRequirements](),

      getMembers[Argument[_]](),
      getMembers[BooleanArgument](),
      getMembers[BooleanTrueArgument](),
      getMembers[BooleanFalseArgument](),
      getMembers[DoubleArgument](),
      getMembers[FileArgument](),
      getMembers[IntegerArgument](),
      getMembers[LongArgument](),
      getMembers[StringArgument](),
    )
  }
  object memberData_part2 { 
    val part = List(
      getMembers[Resource](),
      getMembers[BashScript](),
      getMembers[CSharpScript](),
      getMembers[Executable](),
      getMembers[JavaScriptScript](),
      getMembers[NextflowScript](),
      getMembers[PlainFile](),
      getMembers[PythonScript](),
      getMembers[RScript](),
      getMembers[ScalaScript](),

      getMembers[NextflowDirectives](),
      getMembers[NextflowAuto](),
      getMembers[NextflowConfig](),

      getMembers[Dependency](),
      getMembers[Repository](),
      getMembers[LocalRepository](),
      getMembers[GitRepository](),
      getMembers[GithubRepository](),
      getMembers[ViashhubRepository](),
      getMembers[RepositoryWithName](),
      getMembers[LocalRepositoryWithName](),
      getMembers[GitRepositoryWithName](),
      getMembers[GithubRepositoryWithName](),
      getMembers[ViashhubRepositoryWithName](),
    )
  }

  private def trimTypeName(s: String) = {
    // first: io.viash.helpers.data_structures.OneOrMore[String] -> OneOrMore[String]
    // second: List[io.viash.platforms.requirements.Requirements] -> List[Requirements]
    // third: Either[String,io.viash.functionality.dependencies.Repository] -> Either[String,Repository]
    s
      .replaceAll("""^(\w*\.)*""", "")
      .replaceAll("""(\w*)\[[\w\.]*?(\w*)(\[_\])?\]""", "$1[$2]")
      .replaceAll("""(\w*)\[[\w\.]*?(\w*),[\w\.]*?(\w*)\]""", "$1[$2,$3]")
  }

  val fullData = memberData_part1.part ++ memberData_part2.part

  // Main call for documentation output
  lazy val data: List[List[ParameterSchema]] = fullData.map(_.filter(p => !p.hasUndocumented && !p.hasInternalFunctionality))

  def getKeyFromParamList(data: List[ParameterSchema]): String = data.find(p => p.name == "__this__").get.`type`

  def getJson: Json = data.asJson

  // Main call for checking whether all arguments are annotated with a description
  // Add extra non-annotated value so we can always somewhat check the code is functional
  def getAllNonAnnotated: List[(String, String)] = (data :+ getMembers[DeprecatedOrRemovedSchema]()).flatMap {
    members => {
      val notAnnonated = members.filter(p => p.description == None)
      val thisType = members.find(p => p.name == "__this__").get.`type`
      notAnnonated.map(p => (thisType, p.name))
    }
  }

  def getAllDeprecations: Map[String, DeprecatedOrRemovedSchema] = {
    val arr = data.flatMap(v => v.map(p => (s"config ${getKeyFromParamList(v)} ${p.name}", p.deprecated))).toMap    
    arr.filter(t => t._2.isDefined).map(t => (t._1, t._2.get))
  }

}
