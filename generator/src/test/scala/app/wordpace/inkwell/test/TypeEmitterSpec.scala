package app.wordpace.inkwell.test

import java.time.{LocalDate, LocalDateTime}

import app.wordpace.inkwell.generator.{Import, ImportSimplifyingTypeEmitter}
import app.wordpace.inkwell.test.TypeEmitterSpec.traits.Trait
import app.wordpace.inkwell.test.TypeEmitterSpec.{CaseClass, ParamAlias, SimpleAlias}
import org.scalatest.{FlatSpec, Matchers}

import scala.reflect.runtime.universe.typeOf

object TypeEmitterSpec {
  type SimpleAlias = Boolean
  type ParamAlias[A, B] = Map[A, B]
  case class CaseClass()

  object traits {
    trait Trait
  }
}

class TypeEmitterSpec extends FlatSpec with Matchers {

  private def basicEmitter() = new ImportSimplifyingTypeEmitter(Set(
    Import.Package("app.wordpace.inkwell.test.TypeEmitterSpec"),
  ))

  "ImportSimplifyingTypeEmitter" should "build primitive types correctly" in {
    val emitter = basicEmitter()
    emitter.fromType(typeOf[Boolean]) shouldEqual "Boolean"
    emitter.fromType(typeOf[Byte]) shouldEqual "Byte"
    emitter.fromType(typeOf[Short]) shouldEqual "Short"
    emitter.fromType(typeOf[Int]) shouldEqual "Int"
    emitter.fromType(typeOf[Long]) shouldEqual "Long"
    emitter.fromType(typeOf[Float]) shouldEqual "Float"
    emitter.fromType(typeOf[Double]) shouldEqual "Double"
    emitter.fromType(typeOf[Char]) shouldEqual "Char"
  }

  it should "build array types correctly" in {
    val emitter = basicEmitter()
    emitter.fromType(typeOf[Array[Boolean]]) shouldEqual "Array[Boolean]"
    emitter.fromType(typeOf[Array[Int]]) shouldEqual "Array[Int]"
    emitter.fromType(typeOf[Array[String]]) shouldEqual "Array[String]"
    emitter.fromType(typeOf[Array[CaseClass]]) shouldEqual "Array[CaseClass]"
  }

  it should "preserve aliases" in {
    val emitter = basicEmitter()
    emitter.fromType(typeOf[SimpleAlias]) shouldEqual "SimpleAlias"
    emitter.fromType(typeOf[Array[SimpleAlias]]) shouldEqual "Array[SimpleAlias]"
    emitter.fromType(typeOf[ParamAlias[String, Seq[String]]]) shouldEqual
     "ParamAlias[String, Seq[String]]"
  }

  it should "simplify imported names correctly" in {
    val emitter = new ImportSimplifyingTypeEmitter(Set(
      Import.Package("app.wordpace.inkwell.test.TypeEmitterSpec"),
      Import.Entity.fromType(typeOf[LocalDateTime]),
    ))

    // CaseClass should be simplified fully while Trait should be simplified up to the traits object.
    emitter.fromType(typeOf[CaseClass]) shouldEqual "CaseClass"
    emitter.fromType(typeOf[Trait]) shouldEqual "traits.Trait"

    // Only LocalDateTime has been imported as an entity, so LocalDate should need to be fully qualified.
    emitter.fromType(typeOf[LocalDateTime]) shouldEqual "LocalDateTime"
    emitter.fromType(typeOf[LocalDate]) shouldEqual "java.time.LocalDate"
  }

}
