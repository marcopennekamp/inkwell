package com.github.choppythelumberjack.trivialgen.generator

import com.github.choppythelumberjack.trivialgen.schema.{Column, TypeResolver}

/**
  * Handles the transformation of one column to a class property.
  */
trait PropertyEmitter {
  // TODO: Support default values.

  /**
    * The column to be transformed.
    */
  def column: Column

  /**
    * The generated code. If all else fails (i.e. overriding name and rawType doesn't do it), override this
    * definition.
    */
  def code: String

  /**
    * The name of the column (possibly transformed by some kind of [[NamingStrategy]]).
    */
  def name: String

  /**
    * The raw type of the column. This can be used to implement "tricky" types (such as parametrized custom
    * types for foreign keys) for which [[TypeResolver]] is insufficient.
    */
  def rawType: String
}

/**
  * Emits a property based on the globally configured naming strategy and without modifying the raw type.
  */
class DefaultPropertyEmitter(config: GeneratorConfiguration, override val column: Column) extends PropertyEmitter {
  override def code: String = s"$name: $rawType"
  override def name: String = config.namingStrategy.property(column.name)
  override def rawType: String = config.rawTypeBuilder(column.dataType)
}
