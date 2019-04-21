package com.github.choppythelumberjack.trivialgen.generator

import com.github.choppythelumberjack.trivialgen.GeneratorConfiguration
import com.github.choppythelumberjack.trivialgen.schema.{Column, TypeResolver}

import scala.reflect.runtime.universe.Type

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
  protected def typeWithNullable: String = if (column.isNullable) s"Option[$rawType]" else rawType

  /**
    * The emitted raw type of the column. Overriding the raw type directly can be used to implement types
    * which can't be represented by [[Type]] at code generation time, such as types which aren't known to
    * the generator at runtime.
    *
    * For example, let's say the application (using this code generator library) defines an Id[A] type,
    * which represents (database) IDs for any type A. Say you have a table <i>person</i> from which a case
    * class Person is generated. You can resolve the JDBC type of its ID column to Id with [[TypeResolver]],
    * but you can not provide the type parameter A = Person since Person does not exist until the generation
    * is finished. In such a case, overriding this definition is a good idea.
    */
  protected def rawType: String

  /**
    * The scala [[Type]] of the column. You can override this definition to change the type "at the last
    * minute" before [[rawType]] is calculated.
    */
  protected def scalaType: Type = column.scalaType
}

/**
  * Emits a property based on the globally configured naming strategy and without locally modifying the raw type.
  */
class DefaultPropertyEmitter(config: GeneratorConfiguration, override val column: Column) extends PropertyEmitter {
  override def rawType: String = config.rawTypeBuilder(scalaType)
  override def namingStrategy: NamingStrategy = config.namingStrategy
}

// TODO: Add an Id property emitter that uses foreign keys and primary keys.
