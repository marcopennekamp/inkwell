package com.github.choppythelumberjack.trivialgen.generator

import com.github.choppythelumberjack.trivialgen.schema._

/**
  * Handles the generation of the whole schema and has the power to decide in which files, objects or even
  * packages specific classes are placed.
  */
trait SchemaEmitter {

}

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

/**
  * Handles the transformation of one column to a class property.
  */
trait PropertyEmitter {
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
  override def rawType: String = column.dataType.toString
}
