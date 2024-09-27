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
import io.viash.schemas.internalFunctionality

inline def fieldsOf[T]: List[String] = ${ fieldsOfImpl[T] }
inline def niceNameOf[T]: String = ${ niceNameImpl[T] }
inline def fieldsInternalFunctionality[T]: List[String] = ${ fieldsInternalFunctionalityImpl[T] }

def fieldsOfImpl[T: Type](using Quotes): Expr[List[String]] =
  import quotes.reflect.*
  val tpe = TypeRepr.of[T].typeSymbol
  val fieldSymbols = tpe.caseFields.map(_.name)
  Expr(fieldSymbols)

def niceNameImpl[T: Type](using Quotes): Expr[String] =
  import quotes.reflect.*
  val name = TypeRepr.of[T].typeSymbol.name
  Expr(name)

// TODO this doesn't get annotations from parent classes
def fieldsInternalFunctionalityImpl[T: Type](using Quotes): Expr[List[String]] =
  import quotes.reflect.*
  val annot = TypeRepr.of[internalFunctionality].typeSymbol
  val fieldSymbols = TypeRepr
    .of[T]
    .baseClasses
    .flatMap(_.declaredFields)
    .collect{case f if f.hasAnnotation(annot) => f.name }
  Expr(fieldSymbols)
