package com.github.choppythelumberjack.trivialgen.test

import java.time.{LocalDate, LocalDateTime}

import com.github.choppythelumberjack.trivialgen.generator.{Import, ImportSimplifyingRawTypeBuilder}
import com.github.choppythelumberjack.trivialgen.test.RawTypeBuilderSpec.traits.Trait
import com.github.choppythelumberjack.trivialgen.test.RawTypeBuilderSpec.{CaseClass, ParamAlias, SimpleAlias}
import org.scalatest.{FlatSpec, Matchers}

import scala.reflect.runtime.universe.typeOf

object RawTypeBuilderSpec {
  type SimpleAlias = Boolean
  type ParamAlias[A, B] = Map[A, B]
  case class CaseClass()

  object traits {
    trait Trait
  }
}

class RawTypeBuilderSpec extends FlatSpec with Matchers {

  private def basicBuilder() = new ImportSimplifyingRawTypeBuilder(Set(
    Import.Package("com.github.choppythelumberjack.trivialgen.test.RawTypeBuilderSpec"),
  ))

  "ImportSimplifyingRawTypeBuilder" should "build primitive types correctly" in {
    val builder = basicBuilder()
    builder(typeOf[Boolean]) shouldEqual "Boolean"
    builder(typeOf[Byte]) shouldEqual "Byte"
    builder(typeOf[Short]) shouldEqual "Short"
    builder(typeOf[Int]) shouldEqual "Int"
    builder(typeOf[Long]) shouldEqual "Long"
    builder(typeOf[Float]) shouldEqual "Float"
    builder(typeOf[Double]) shouldEqual "Double"
    builder(typeOf[Char]) shouldEqual "Char"
  }

  it should "build array types correctly" in {
    val builder = basicBuilder()
    builder(typeOf[Array[Boolean]]) shouldEqual "Array[Boolean]"
    builder(typeOf[Array[Int]]) shouldEqual "Array[Int]"
    builder(typeOf[Array[String]]) shouldEqual "Array[String]"
    builder(typeOf[Array[CaseClass]]) shouldEqual "Array[CaseClass]"
  }

  it should "preserve aliases" in {
    val builder = basicBuilder()
    builder(typeOf[SimpleAlias]) shouldEqual "SimpleAlias"
    builder(typeOf[Array[SimpleAlias]]) shouldEqual "Array[SimpleAlias]"
    builder(typeOf[ParamAlias[String, Seq[String]]]) shouldEqual
     "ParamAlias[String, Seq[String]]"
  }

  it should "simplify imported names correctly" in {
    val builder = new ImportSimplifyingRawTypeBuilder(Set(
      Import.Package("com.github.choppythelumberjack.trivialgen.test.RawTypeBuilderSpec"),
      Import.Entity.fromType(typeOf[LocalDateTime]),
    ))

    // CaseClass should be simplified fully while Trait should be simplified up to the traits object.
    builder(typeOf[CaseClass]) shouldEqual "CaseClass"
    builder(typeOf[Trait]) shouldEqual "traits.Trait"

    // Only LocalDateTime has been imported as an entity, so LocalDate should need to be fully qualified.
    builder(typeOf[LocalDateTime]) shouldEqual "LocalDateTime"
    builder(typeOf[LocalDate]) shouldEqual "java.time.LocalDate"
  }

}
