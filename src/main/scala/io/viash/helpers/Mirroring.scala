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

package io.viash.helpers

import scala.quoted.*
import io.viash.schemas.*

inline def typeOf[T]: String = ${ typeOfImpl[T] }
inline def deprecatedOf[T]: Vector[(String, String, String)] = ${ deprecatedOfImpl[T] }
inline def removedOf[T]: Vector[(String, String, String)] = ${ removedOfImpl[T] }

inline def fieldsOf[T]: List[String] = ${ fieldsOfImpl[T] }
inline def internalFunctionalityFieldsOf[T]: List[String] = ${ internalFunctionalityFieldsOfImpl[T] }
inline def deprecatedFieldsOf[T]: Vector[(String, String, String, String)] = ${ deprecatedFieldsOfImpl[T] }
inline def removedFieldsOf[T]: Vector[(String, String, String, String)] = ${ removedFieldsOfImpl[T] }
inline def annotationsOf[T]: List[(String, List[String])] = ${ annotationsOfImpl[T] }
inline def membersOf[T]: List[String] = ${ membersOfImpl[T] }
inline def memberAnnotationsOf[T]: List[(String, List[(String, List[String])])] = ${ memberAnnotationsOfImpl[T] }
inline def historyOf[T]: List[String] = ${ historyOfImpl[T] }

def typeOfImpl[T: Type](using Quotes): Expr[String] =
  import quotes.reflect.*
  val typeRepr = TypeRepr.of[T]

  // Use pattern matching to extract a simplified name
  def simpleName(tpe: TypeRepr): String = tpe match {
    case AppliedType(tycon, args) =>
      // If it's a type constructor with arguments, show it in a readable form
      s"${simpleName(tycon)}[${args.map(simpleName).mkString(", ")}]"
    case _ =>
      // Strip the full package name to get the simple type name
      tpe.typeSymbol.name
  }
  Expr(simpleName(typeRepr))

def deprecatedOfImpl[T](using Type[T], Quotes): Expr[Vector[(String, String, String)]] =
  import quotes.reflect.*
  val annot = TypeRepr.of[deprecated].typeSymbol
  val tuple = TypeRepr
    .of[T]
    .typeSymbol
    .getAnnotation(annot)
    .map:
      case annot =>
        val annotExpr = annot.asExprOf[deprecated]
        '{ ($annotExpr.message, $annotExpr.since, $annotExpr.plannedRemoval) }
  val list = tuple match {
    case Some(t) => Seq(t)
    case None => Nil
  }
  val seq: Expr[Seq[(String, String, String)]] = Expr.ofSeq(list)
  '{ $seq.toVector }

def removedOfImpl[T](using Type[T], Quotes): Expr[Vector[(String, String, String)]] =
  import quotes.reflect.*
  val annot = TypeRepr.of[removed].typeSymbol
  val tuple = TypeRepr
    .of[T]
    .typeSymbol
    .getAnnotation(annot)
    .map:
      case annot =>
        val annotExpr = annot.asExprOf[removed]
        '{ ($annotExpr.message, $annotExpr.deprecatedSince, $annotExpr.since) }
  val list = tuple match {
    case Some(t) => Seq(t)
    case None => Nil
  }
  val seq: Expr[Seq[(String, String, String)]] = Expr.ofSeq(list)
  '{ $seq.toVector }

def fieldsOfImpl[T: Type](using Quotes): Expr[List[String]] =
  import quotes.reflect.*
  val tpe = TypeRepr.of[T].typeSymbol
  val fieldSymbols = tpe.caseFields.map(_.name)
  Expr(fieldSymbols)

def internalFunctionalityFieldsOfImpl[T: Type](using Quotes): Expr[List[String]] =
  import quotes.reflect.*
  val annot = TypeRepr.of[internalFunctionality].typeSymbol
  val fieldSymbols = TypeRepr
    .of[T]
    .baseClasses
    .flatMap(_.declaredFields)
    .collect{case f if f.hasAnnotation(annot) => f.name }
  Expr(fieldSymbols)

def deprecatedFieldsOfImpl[T: Type](using Quotes): Expr[Vector[(String, String, String, String)]] =
  import quotes.reflect.*
  val annot = TypeRepr.of[deprecated].typeSymbol
  val tuples = TypeRepr
    .of[T]
    .baseClasses
    .flatMap(_.declaredFields)
    .collect:
      case f if f.hasAnnotation(annot) =>
        val fieldNameExpr = Expr(f.name.asInstanceOf[String])
        val annotExpr = f.getAnnotation(annot).get.asExprOf[deprecated]
        '{ ($fieldNameExpr, $annotExpr.message, $annotExpr.since, $annotExpr.plannedRemoval) }
  val seq: Expr[Seq[(String, String, String, String)]] = Expr.ofSeq(tuples)
  '{ $seq.toVector }

def removedFieldsOfImpl[T: Type](using Quotes): Expr[Vector[(String, String, String, String)]] =
  import quotes.reflect.*
  val annot = TypeRepr.of[removed].typeSymbol
  val tuples = TypeRepr
    .of[T]
    .baseClasses
    .flatMap(_.declaredFields)
    .collect:
      case f if f.hasAnnotation(annot) =>
        val fieldNameExpr = Expr(f.name.asInstanceOf[String])
        val annotExpr = f.getAnnotation(annot).get.asExprOf[removed]
        '{ ($fieldNameExpr, $annotExpr.message, $annotExpr.deprecatedSince, $annotExpr.since) }
  val seq: Expr[Seq[(String, String, String, String)]] = Expr.ofSeq(tuples)
  '{ $seq.toVector }



def annotationsOfImpl[T: Type](using Quotes): Expr[List[(String, List[String])]] =
  import quotes.reflect.*
  val tpe = TypeRepr.of[T].typeSymbol


  def unfinishedStringStripMargin(s: String, marginChar: Char = '|'): String = {
    s.replaceAll("\\\\n", "\n").stripMargin(marginChar)
  }

  def mapTreeList(l: List[Tree], marginChar: Char = '|'): String = {
    l.map(i => i match {
      // case Literal(Constant(value: String)) =>
      //   unfinishedStringStripMargin(value, marginChar)
      case Literal(value) =>
        unfinishedStringStripMargin(value.show(using Printer.ConstantCode), marginChar)
      case _ =>
        "unmatched in mapTreeList: " + i.toString()
    }).mkString
  }

  // Traverse tree information and extract values or lists of values
  def annotationToStrings(ann: Term): List[String] = {
    // val name = ann.tree.tpe.toString()
    val values = ann match {
      case Apply(c, args: List[Tree]) =>
        args.collect({
          case i: Tree =>
            i match {
              // Here 'Apply' contains lists
              // While 'Select' has a single element
              // case Literal(Constant(value: String)) =>
              //   value
              case Literal(value) =>
                value.show(using Printer.ConstantCode)
              // case Select(Select(a, b), stripMargin) =>
              //   unfinishedStringStripMargin(b)
              case Select(Apply(a, a2), b) if b.toString == "stripMargin" =>
                mapTreeList(a2)
              case Apply(Select(Apply(a, a2), b), stripMargin) if b.toString == "stripMargin" =>
                val stripper = stripMargin.head.toString.charAt(1)
                mapTreeList(a2, stripper)
              case _ =>
                "unmatched in annotationToStrings: " + i.toString()
            }
        })
    }
    values
  }

  val annots = tpe.annotations
    .filter(_.tpe.typeSymbol.fullName.startsWith("io.viash"))
    .map(ann => (ann.tpe.typeSymbol.name, annotationToStrings(ann)))

  Expr(annots)

def membersOfImpl[T: Type](using Quotes): Expr[List[String]] = {
  import quotes.reflect.*
  val tpe = TypeRepr.of[T].typeSymbol
  val memberSymbols = tpe.fieldMembers.map(_.name)
  Expr(memberSymbols)
}

def memberAnnotationsOfImpl[T: Type](using Quotes): Expr[List[(String, List[(String, List[String])])]] = {
  import quotes.reflect.*
  val tpe = TypeRepr.of[T].typeSymbol
  val memberSymbols = tpe.fieldMembers

  def unfinishedStringStripMargin(s: String, marginChar: Char = '|'): String = {
    s.replaceAll("\\\\n", "\n").stripMargin(marginChar)
  }

  def mapTreeList(l: List[Tree], marginChar: Char = '|'): String = {
    l.map(i => i match {
      // case Literal(Constant(value: String)) =>
      //   unfinishedStringStripMargin(value, marginChar)
      case Literal(value) =>
        unfinishedStringStripMargin(value.show(using Printer.ConstantCode), marginChar)
      case _ =>
        "unmatched in mapTreeList: " + i.toString()
    }).mkString
  }

  // Traverse tree information and extract values or lists of values
  def annotationToStrings(ann: Term): List[String] = {
    // val name = ann.tree.tpe.toString()
    val values = ann match {
      case Apply(c, args: List[Tree]) =>
        args.collect({
          case i: Tree =>
            i match {
              // Here 'Apply' contains lists
              // While 'Select' has a single element
              // case Literal(Constant(value: String)) =>
              //   value
              case Literal(value) =>
                value.show(using Printer.ConstantCode)
              // case Select(Select(a, b), stripMargin) =>
              //   unfinishedStringStripMargin(b)
              case Select(Apply(a, a2), b) if b.toString == "stripMargin" =>
                mapTreeList(a2)
              case Apply(Select(Apply(a, a2), b), stripMargin) if b.toString == "stripMargin" =>
                val stripper = stripMargin.head.toString.charAt(1)
                mapTreeList(a2, stripper)
              case _ =>
                "unmatched in annotationToStrings: " + i.toString()
            }
        })
    }
    values
  }

  val annots = 
    memberSymbols
      .map{ case m => 
        val name = m.name
        val n: Symbol = m
        val annotations = m.annotations
          .filter(_.tpe.typeSymbol.fullName.startsWith("io.viash"))
          .map(ann => (ann.tpe.typeSymbol.name, annotationToStrings(ann)))
        (name, annotations)
      }

  Expr(annots)
}

def historyOfImpl[T: Type](using Quotes): Expr[List[String]] = {
  import quotes.reflect.*
  val baseClasses = TypeRepr.of[T].baseClasses.map(_.fullName).filter(_.startsWith("io.viash"))

  Expr(baseClasses)
}