package app.wordpace.inkwell.generator

import app.wordpace.inkwell.GeneratorConfiguration
import app.wordpace.inkwell.schema.Column

/**
  * Handles the transformation of one column to a class property.
  */
trait PropertyEmitter {
  /**
    * The column to be transformed.
    */
  protected def column: Column

  /**
    * The generated code. Please ensure that the generated name and type are consistent with the naming strategy
    * and type emitter.
    */
  def code: String = s"${column.scalaName}: $typeWithNullable${defaultValue.map(v => s" = $v").getOrElse("")}"

  /**
    * The naming strategy for the property name.
    */
  protected implicit def namingStrategy: NamingStrategy

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

  /**
    * The default value of the property.
    *
    * By default, only nullables are translated to a Scala default value. If you need to translate other
    * default values, please override this method.
    */
  protected def defaultValue: Option[String] = {
    if (column.isNullable) Some("None") else None // What a fun piece of code.
  }
}

/**
  * Emits a property based on the globally configured naming strategy and without locally modifying the raw type.
  */
class DefaultPropertyEmitter(config: GeneratorConfiguration, override val column: Column) extends PropertyEmitter {
  override implicit def namingStrategy: NamingStrategy = config.namingStrategy
  override def typeEmitter: TypeEmitter = config.typeEmitter
}
