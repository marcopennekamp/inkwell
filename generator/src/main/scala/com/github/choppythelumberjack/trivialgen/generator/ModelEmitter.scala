package com.github.choppythelumberjack.trivialgen.generator

import com.github.choppythelumberjack.trivialgen.generator.DefaultModelEmitter.InheritanceMap
import com.github.choppythelumberjack.trivialgen.schema.Table

import scala.reflect.ClassTag

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
  def code: String

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
  *
  * Note that the inheritance map points from SQL names and <b>not</b> from the Scala name (after
  * applying the naming strategy).
  */
class DefaultModelEmitter(
  config: GeneratorConfiguration,
  inheritanceMap: InheritanceMap,
  override val table: Table
) extends ModelEmitter {
  override def code: String =
    s"""
       |case class $name(${properties.mkString(", ")}) $extendsClause
    """.stripMargin

  def extendsClause: String = {
    if (supertypes.nonEmpty) {
      (s"extends ${supertypes.head}" :: supertypes.tail.toList).mkString(" with ")
    } else {
      ""
    }
  }

  override def name: String = config.namingStrategy.model(table.name)
  override def properties: Seq[String] = table.columns.map(c => config.selectPropertyEmitter(c).code)
  override def supertypes: Seq[String] = inheritanceMap.getOrElse(table.name, Seq.empty).map(_.toString)
}

object DefaultModelEmitter {
  type InheritanceMap = Map[Table.Name, Seq[ClassTag[_]]]
}
