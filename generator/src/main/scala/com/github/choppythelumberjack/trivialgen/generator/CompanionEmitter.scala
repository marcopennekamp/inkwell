package com.github.choppythelumberjack.trivialgen.generator

import com.github.choppythelumberjack.trivialgen.GeneratorConfiguration
import com.github.choppythelumberjack.trivialgen.schema.Table

/**
  * Handles the transformation of one table to a model's companion object.
  */
trait CompanionEmitter {
  /**
    * The table to be transformed.
    */
  protected def table: Table

  /**
    * The generated code for the companion object. Please ensure that the name is consistent with the naming strategy.
    */
  def code: String = if (innerCode.isEmpty) "" else
    s"""object ${namingStrategy.model(table)} {
       |  $innerCode
       |}""".stripMargin

  /**
    * The naming strategy for the companion name.
    */
  def namingStrategy: NamingStrategy

  /**
    * The inner code of the companion object.
    */
  protected def innerCode: String
}

// TODO: Add inheritance support for the companion object? It's useful in cases where pre-defined implicits
//       have to be associated with an object, or in general definitions. See also SchemaTests for an example
//       of a good use case with the age quote.

class DefaultCompanionEmitter(config: GeneratorConfiguration, override val table: Table) extends CompanionEmitter {
  override def namingStrategy: NamingStrategy = config.namingStrategy
  override def innerCode: String = ""
}
