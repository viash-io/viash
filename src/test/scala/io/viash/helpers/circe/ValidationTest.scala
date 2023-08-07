package io.viash.helpers.circe

import org.scalatest.BeforeAndAfterAll
import org.scalatest.funsuite.AnyFunSuite
import io.circe._
import io.circe.yaml.parser
import io.viash.helpers.circe._
import io.circe.generic.extras.semiauto.{deriveConfiguredDecoder, deriveConfiguredEncoder}
import io.viash.helpers.data_structures._
import io.viash.helpers.Logger
import java.io.ByteArrayOutputStream
import io.viash.schemas._
import io.viash.exceptions.ConfigParserValidationException

class ValidationTest extends AnyFunSuite {
  Logger.UseColorOverride.value = Some(false)
  
  @deprecated("testing class deprecation.", "0.1.2", "987.654.321")
  case class TestClassDeprecation(
    foo: String
  )
  @removed("testing class removal.", "0.0.1", "0.1.2")
  case class TestClassRemoval(
    foo: String
  )
  case class TestClassWithFieldDeprecation(
    @deprecated("testing deprecation of foo.", "0.1.2", "987.654.321")
    foo: String
  )
  case class TestClassWithFieldRemoval(
    @removed("testing removal of foo.", "0.0.1", "0.1.2")
    foo: String
  )
  case class TestClassValidation(
    bar: String
  )
  implicit val decodeDeprecation: Decoder[TestClassDeprecation] = DeriveConfiguredDecoderWithDeprecationCheck.deriveConfiguredDecoderWithDeprecationCheck
  implicit val decodeRemoval: Decoder[TestClassRemoval] = DeriveConfiguredDecoderWithDeprecationCheck.deriveConfiguredDecoderWithDeprecationCheck
  implicit val decodeFieldDeprecation: Decoder[TestClassWithFieldDeprecation] = DeriveConfiguredDecoderWithDeprecationCheck.deriveConfiguredDecoderWithDeprecationCheck
  implicit val decodeFieldRemoval: Decoder[TestClassWithFieldRemoval] = DeriveConfiguredDecoderWithDeprecationCheck.deriveConfiguredDecoderWithDeprecationCheck
  implicit val decodeValidation: Decoder[TestClassValidation] = DeriveConfiguredDecoderWithValidationCheck.deriveConfiguredDecoderWithValidationCheck

  test("parsing of a deprecated class") {
    val json = parser.parse("foo: bar").getOrElse(Json.Null)

    val outStream = new ByteArrayOutputStream()
    val errStream = new ByteArrayOutputStream()
    val parsed = Console.withOut(outStream) {
      Console.withErr(errStream) {
        json.as[TestClassDeprecation].toOption.get
      }
    }

    val stdout = outStream.toString
    val stderr = errStream.toString

    assert(stdout.isEmpty())
    assert(stderr.contains("Warning: TestClassDeprecation is deprecated: testing class deprecation. Deprecated since 0.1.2, planned removal 987.654.321."))

    assert(parsed == TestClassDeprecation(foo = "bar"))
  }

  test("parsing of a removed class") {
    val json = parser.parse("foo: bar").getOrElse(Json.Null)

    val outStream = new ByteArrayOutputStream()
    val errStream = new ByteArrayOutputStream()
    val parsed = Console.withOut(outStream) {
      Console.withErr(errStream) {
        json.as[TestClassRemoval].toOption.get
      }
    }

    val stdout = outStream.toString
    val stderr = errStream.toString

    assert(stdout.isEmpty())
    assert(stderr.contains("Error: TestClassRemoval was removed: testing class removal. Initially deprecated 0.0.1, removed 0.1.2."))

    assert(parsed == TestClassRemoval(foo = "bar"))
  }


  test("parsing of a class with a deprecated field") {
    val json = parser.parse("foo: bar").getOrElse(Json.Null)

    val outStream = new ByteArrayOutputStream()
    val errStream = new ByteArrayOutputStream()
    val parsed = Console.withOut(outStream) {
      Console.withErr(errStream) {
        json.as[TestClassWithFieldDeprecation].toOption.get
      }
    }

    val stdout = outStream.toString
    val stderr = errStream.toString

    assert(stdout.isEmpty())
    assert(stderr.contains("Warning: ..foo is deprecated: testing deprecation of foo. Deprecated since 0.1.2, planned removal 987.654.321."))

    assert(parsed == TestClassWithFieldDeprecation(foo = "bar"))
  }

  test("parsing of a class with a removed field") {
    val json = parser.parse("foo: bar").getOrElse(Json.Null)

    val outStream = new ByteArrayOutputStream()
    val errStream = new ByteArrayOutputStream()
    val parsed = Console.withOut(outStream) {
      Console.withErr(errStream) {
        json.as[TestClassWithFieldRemoval].toOption.get
      }
    }

    val stdout = outStream.toString
    val stderr = errStream.toString

    assert(stdout.isEmpty())
    assert(stderr.contains("Error: ..foo was removed: testing removal of foo. Initially deprecated 0.0.1, removed 0.1.2."))

    assert(parsed == TestClassWithFieldRemoval(foo = "bar"))
  }

  test("parsing of a structure that does not match class definition") {
    val json = parser.parse("foo: bar").getOrElse(Json.Null)

    val outStream = new ByteArrayOutputStream()
    val errStream = new ByteArrayOutputStream()
    val exception = intercept[ConfigParserValidationException] {
      Console.withOut(outStream) {
        Console.withErr(errStream) {
          json.as[TestClassValidation].toOption.get
        }
      }
    }

    val stdout = outStream.toString
    val stderr = errStream.toString

    assert(stdout.isEmpty())
    assert(stderr.isEmpty())
    assert(exception.toString().contains("Invalid data fields for TestClassValidation."))
  }

}