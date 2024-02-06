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

import scala.reflect.runtime.universe._
import io.circe.{Encoder, Printer => JsonPrinter}
import io.circe.syntax.EncoderOps
import io.circe.generic.extras.semiauto.deriveConfiguredEncoder

import io.viash.functionality._
import io.viash.runners._
import io.viash.engines._
import io.viash.platforms._
import io.viash.functionality.arguments._
import io.circe.Json
import monocle.function.Cons
import io.viash.config.Config
import io.viash.config.Info
import io.viash.functionality.resources._
import io.viash.packageConfig.PackageConfig
import io.viash.helpers._
import scala.collection.immutable.ListMap
import io.viash.functionality.dependencies._
import io.viash.runners.nextflow.{NextflowConfig, NextflowAuto, NextflowDirectives}
import io.viash.engines.requirements._

final case class CollectedSchemas (
  config: Map[String, List[ParameterSchema]],
  functionality: Map[String, List[ParameterSchema]],
  runners: Map[String, List[ParameterSchema]],
  engines: Map[String, List[ParameterSchema]],
  platforms: Map[String, List[ParameterSchema]],
  requirements: Map[String, List[ParameterSchema]],
  arguments: Map[String, List[ParameterSchema]],
  resources: Map[String, List[ParameterSchema]],
  nextflowParameters: Map[String, List[ParameterSchema]],
)


object CollectedSchemas {

  implicit class RichSymbol(s: Symbol) {
    def shortName = s.fullName.split('.').last
  }

  case class MemberInfo (
    symbol: Symbol,
    inConstructor: Boolean,
    className: String,
    inheritanceIndex: Int
  ) {
    def fullName = symbol.fullName
    def shortName = symbol.shortName
  }

  private val jsonPrinter = JsonPrinter.spaces2.copy(dropNullValues = true)

  import io.viash.helpers.circe._
  import io.viash.helpers.circe.DeriveConfiguredEncoderStrict._

  private implicit val encodeConfigSchema: Encoder.AsObject[CollectedSchemas] = deriveConfiguredEncoder
  private implicit val encodeParameterSchema: Encoder.AsObject[ParameterSchema] = deriveConfiguredEncoderStrict
  private implicit val encodeDeprecatedOrRemoved: Encoder.AsObject[DeprecatedOrRemovedSchema] = deriveConfiguredEncoder
  private implicit val encodeExample: Encoder.AsObject[ExampleSchema] = deriveConfiguredEncoder

  private def getMembers[T: TypeTag](): (Map[String,List[MemberInfo]], List[Symbol]) = {

    val name = typeOf[T].typeSymbol.shortName

    // Get all members and filter for constructors, first one should be the best (most complete) one
    // Traits don't have constructors
    // Get all parameters and store their short name
    val constructorMembers = typeOf[T].members.filter(_.isConstructor).headOption.map(_.asMethod.paramLists.head.map(_.shortName)).getOrElse(List.empty[String])

    val baseClasses = typeOf[T].baseClasses
      .filter(_.fullName.startsWith("io.viash"))

    // If we're only getting a abstract class/trait, not a final implementation, use these definitions (otherwise we're left with nothing).
    val documentFully = 
      baseClasses.length == 1 && 
      baseClasses.head.isAbstract &&
      baseClasses.head.annotations.exists(a => a.tree.tpe =:= typeOf[documentFully])

    val memberNames = typeOf[T].members
      .filter(!_.isMethod || documentFully)
      .map(_.shortName)
      .toSeq

    val allMembers = baseClasses
      .zipWithIndex
      .flatMap{ case (baseClass, index) =>
        baseClass.info.members
          .filter(_.fullName.startsWith("io.viash"))
          .filter(m => memberNames.contains(m.shortName))
          .filter(m => !m.info.getClass.toString.endsWith("NullaryMethodType") || index != 0 || documentFully) // Only regular members if base class, otherwise all members
          .map(y => MemberInfo(y, (constructorMembers.contains(y.shortName)), baseClass.fullName, index))
        }
      .groupBy(k => k.shortName)
    
    (allMembers, baseClasses)
  }

  lazy val schemaClasses = List(
    getMembers[Config](),
    getMembers[PackageConfig](),
    getMembers[Info](),
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

  private def trimTypeName(s: String) = {
    // first: io.viash.helpers.data_structures.OneOrMore[String] -> OneOrMore[String]
    // second: List[io.viash.platforms.requirements.Requirements] -> List[Requirements]
    // third: Either[String,io.viash.functionality.dependencies.Repository] -> Either[String,Repository]
    s
      .replaceAll("""^(\w*\.)*""", "")
      .replaceAll("""(\w*)\[[\w\.]*?(\w*)(\[_\])?\]""", "$1[$2]")
      .replaceAll("""(\w*)\[[\w\.]*?(\w*),[\w\.]*?(\w*)\]""", "$1[$2,$3]")
  }

  private def annotationsOf(members: (Map[String,List[MemberInfo]]), classes: List[Symbol]) = {
    val annMembers = members
      .map{ case (memberName, memberInfo) => { 
        val h = memberInfo.head
        val annotations = memberInfo.flatMap(_.symbol.annotations)
        (h.fullName, h.symbol.info.toString, annotations, h.className, h.inheritanceIndex, Nil)
      } }
      .filter(_._3.length > 0)
    val annThis = ("__this__", classes.head.name.toString(), classes.head.annotations, "", 0, classes.map(_.fullName))
    val allAnnotations = annThis :: annMembers.toList
    allAnnotations
      .map({case (name, tpe, annotations, d, e, hierarchy) => (name, trimTypeName(tpe), hierarchy, annotations)})  // TODO this ignores where the annotation was defined, ie. top level class or super class
  }

  private val getSchema = (t: (Map[String,List[MemberInfo]], List[Symbol])) => t match {
    case (members, classes) => {
      annotationsOf(members, classes).map{ case (name, tpe, hierarchy, annotations) => ParameterSchema(name, tpe, hierarchy, annotations) }
    }
  }

  // get all parameters for a given type, including parent class annotations
  def getParameters[T: TypeTag]() = getSchema(getMembers[T]())

  // Main call for documentation output
  lazy val fullData: List[List[ParameterSchema]] = schemaClasses.map{ v => getSchema(v)}
  lazy val data: List[List[ParameterSchema]] = fullData.map(_.filter(p => !p.hasUndocumented && !p.hasInternalFunctionality))

  def getKeyFromParamList(data: List[ParameterSchema]): String = data.find(p => p.name == "__this__").get.`type`

  def getJson: Json = data.asJson

  private def getNonAnnotated(members: Map[String,List[MemberInfo]], classes: List[Symbol]): List[String] = {
    val issueMembers = members
      .toList
      .filter{ case(k, v) => v.map(m => m.inConstructor).contains(true) } // Only check values that are in a constructor. Annotation may occur on private vals but that is not a requirement.
      .map{ case (k, v) => (k, v.map(_.symbol.annotations.length).sum) } // (name, # annotations)
      .filter(_._2 == 0)
      .map(_._1)

    val ownClassArr = if (classes.head.annotations.length == 0) Seq("__this__") else Nil
    issueMembers ++ ownClassArr
  }

  def getMemberName(members: Map[String,List[MemberInfo]], classes: List[Symbol]): String = classes.head.shortName

  // Main call for checking whether all arguments are annotated
  // Add extra non-annotated value so we can always somewhat check the code is functional
  def getAllNonAnnotated: Map[String, String] = (schemaClasses :+ getMembers[CollectedSchemas]()).flatMap {
    v => getNonAnnotated(v._1, v._2).map((getMemberName(v._1, v._2), _))
  }.toMap

  def getAllDeprecations: Map[String, DeprecatedOrRemovedSchema] = {
    val arr = data.flatMap(v => v.map(p => (s"config ${getKeyFromParamList(v)} ${p.name}", p.deprecated))).toMap    
    arr.filter(t => t._2.isDefined).map(t => (t._1, t._2.get))
  }

}
