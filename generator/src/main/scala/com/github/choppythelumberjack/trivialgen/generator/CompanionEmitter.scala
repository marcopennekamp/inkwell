package com.github.choppythelumberjack.trivialgen.generator

import com.github.choppythelumberjack.trivialgen.schema.Table

/**
  * Handles the transformation of one table to a model's companion object.
  */
trait CompanionEmitter {
  /**
    * The table to be transformed.
    */
  def table: Table

  /**
    * The generated code for the companion object.
    */
  def code: String =
    s"""
       |object $name {
       |  $innerCode
       |}
    """.stripMargin

  /**
    * The name of the companion object.
    */
  def name: String

  /**
    * The inner code of the companion object.
    */
  def innerCode: String
}

class DefaultCompanionEmitter(config: GeneratorConfiguration, override val table: Table) extends CompanionEmitter {
  override def name: String = config.namingStrategy.model(table.name)
  override def innerCode: String = ""
}
