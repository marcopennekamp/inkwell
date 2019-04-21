package com.github.choppythelumberjack.trivialgen.generator

import com.github.choppythelumberjack.trivialgen.GeneratorConfiguration
import com.github.choppythelumberjack.trivialgen.schema.Column

/**
  * Handles the transformation of one column to a class property.
  */
trait PropertyEmitter {
  // TODO: Support default values.

  /**
    * The column to be transformed.
    */
  protected def column: Column

  /**
    * The generated code. Please ensure that the generated name and type are consistent with the naming strategy
    * and type emitter.
    */
  def code: String = s"${namingStrategy.property(column)}: $typeWithNullable"

  /**
    * The naming strategy for the property name.
    */
  def namingStrategy: NamingStrategy

  /**
    * The emitted raw type but wrapped in an Option <b>if</b> the column is nullable.
    */
  protected def typeWithNullable: String = {
    val rawType = typeEmitter.fromColumn(column)
    if (column.isNullable) s"Option[$rawType]" else rawType
  }

  /**
    * The type emitter used for the property type.
    */
  protected def typeEmitter: TypeEmitter
}

/**
  * Emits a property based on the globally configured naming strategy and without locally modifying the raw type.
  */
class DefaultPropertyEmitter(config: GeneratorConfiguration, override val column: Column) extends PropertyEmitter {
  override def namingStrategy: NamingStrategy = config.namingStrategy
  override def typeEmitter: TypeEmitter = config.typeEmitter
}
