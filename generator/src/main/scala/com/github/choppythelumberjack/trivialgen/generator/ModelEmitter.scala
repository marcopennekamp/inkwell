package com.github.choppythelumberjack.trivialgen.generator

import com.github.choppythelumberjack.trivialgen.GeneratorConfiguration
import com.github.choppythelumberjack.trivialgen.schema.Table

import scala.reflect.runtime.universe.Type

/**
  * Handles the transformation of one table to a (case) class.
  */
trait ModelEmitter {
  /**
    * The table to be transformed.
    */
  def table: Table

  /**
    * The generated code for the case class.
    */
  def code: String = s"case class $name(${properties.mkString(", ")}) $extendsClause"

  /**
    * The emitted extends clause of the case class declaration.
    */
  def extendsClause: String = {
    if (supertypes.nonEmpty) {
      (s"extends ${supertypes.head}" :: supertypes.tail.toList).mkString(" with ")
    } else {
      ""
    }
  }

  /**
    * The name of the model.
    */
  def name: String

  /**
    * The (emitted) properties of the case class.
    */
  def properties: Seq[String]

  /**
    * The (emitted) supertypes of the case class.
    */
  def supertypes: Seq[String]
}

/**
  * Generates a simple case class based on the configured naming strategy, selected property emitter
  * and inheritance configurations.
  */
class DefaultModelEmitter(
  config: GeneratorConfiguration,
  schemaInheritances: SchemaInheritances,
  override val table: Table
) extends ModelEmitter {
  override def name: String = config.namingStrategy.model(table.name)
  override def properties: Seq[String] = table.columns.map(c => config.selectPropertyEmitter(c).code)
  override def supertypes: Seq[String] = {
    val inh = schemaInheritances.get(table.name)
    inh.types.map(config.rawTypeBuilder(_)) ++ inh.fullNames.map(config.rawTypeBuilder(_))
  }
}
