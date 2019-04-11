package com.github.choppythelumberjack.trivialgen.generator

import com.github.choppythelumberjack.trivialgen.schema.Table

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

class DefaultModelEmitter(config: GeneratorConfiguration, override val table: Table) extends ModelEmitter {
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
  override def supertypes: Seq[String] = Seq.empty // TODO: Use an inheritance table from the config.
}
