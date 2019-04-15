package com.github.choppythelumberjack.trivialgen.test

import com.github.choppythelumberjack.trivialgen.generator.{Import, ImportSimplifyingRawTypeBuilder}
import com.github.choppythelumberjack.trivialgen.test.RawTypeBuilderSpec.{CaseClass, ParamAlias, SimpleAlias}
import org.scalatest.{FlatSpec, Matchers}

import scala.reflect.runtime.universe.typeOf

object RawTypeBuilderSpec {
  type SimpleAlias = Boolean
  type ParamAlias[A, B] = Map[A, B]
  case class CaseClass()
}

class RawTypeBuilderSpec extends FlatSpec with Matchers {

  private def basicBuilder() = new ImportSimplifyingRawTypeBuilder(Set(
    Import.Package("com.github.choppythelumberjack.trivialgen.test.RawTypeBuilderSpec")
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
    // TODO: Implement
  }

}
