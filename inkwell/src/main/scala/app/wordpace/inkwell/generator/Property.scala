package app.wordpace.inkwell.generator

import app.wordpace.inkwell.GeneratorConfiguration
import app.wordpace.inkwell.schema.Column

import scala.reflect.runtime.universe._

trait Property {
  /**
    * The column to be transformed.
    */
  def column: Column

  /**
    * The model that owns the property.
    */
  def model: Model

  /**
    * The name of the property.
    */
  def name: String

  /**
    * The data type of the property. Even though [[Column]] already provides a type reference, you can override this
    * definition to provide an updated type based on the property without overriding [[tpe]].
    */
  def dataType: TypeReference = column.dataType

  /**
    * The scala type of the property. Sometimes, this type may be a wrapped version of [[dataType]], such as the data
    * type wrapped in an Option for nullable columns.
    */
  def tpe: TypeReference

  /**
    * The default value of the property.
    */
  def defaultValue: Option[String]
}

case class DefaultProperty(
  override val column: Column,
  override val model: Model,
  config: GeneratorConfiguration,
) extends Property {
  override def name: String = config.namingStrategy.property(column)

  override def tpe: TypeReference = {
    if (column.isNullable) {
      ScalaTypeReference(typeOf[Option[_]], Seq(dataType))
    } else dataType
  }

  /**
    * The default value of the property.
    *
    * By default, only nullables are translated to a Scala default value. If you need to translate other
    * default values, please override this method.
    */
  override def defaultValue: Option[String] = {
    if (column.isNullable) Some("None") else None // What a fun piece of code.
  }
}

/**
  * This property class resolves primary and foreign key columns to the correct type reference:
  *
  *   (1) A single-column primary key of a type `A` is resolved to a type `id(A)`, where `id` is an overridable
  *       type reference constructor with no default. For example, for a table `person` with a primary key column
  *       `id`, a property `id: Id[Person]` would be emitted if the `id(A)` type was `Id[A]`.
  *   (2) A foreign key pointing to a single-column primary or unique key (see [[Column.references]]) is resolved
  *       to the data type of the referenced property. For example, for a table `employees` with a foreign key
  *       column `person_id`, a property `personId: Id[Person]` would be emitted, using the type `Id[Person]` of
  *       the referenced property.
  *
  * For a primary key that is also a foreign key, case (2) prevails, since the primary key is then interpreted
  * as the primary key of another table, just replicated here due to a one-to-one relationship. (This is valid
  * in a few edge cases where the original table can't or shouldn't be manipulated to add more columns.)
  *
  * <b>Usage Note:</b> For Quill support, your id type should either extend AnyVal (which should suffice
  * in most use cases) or come with an encoder and decoder.
  *
  * In any case, this property only handles single-column primary and foreign keys. You will have to implement
  * some individual handling if you want to type multi-column keys.
  */
abstract class KeyAsIdProperty(
  override val column: Column,
  override val model: Model,
  override val config: GeneratorConfiguration,
) extends DefaultProperty(column, model, config) {
  /**
    * Wraps the given model type reference in your Id type.
    */
  protected def id(modelType: TypeReference): TypeReference

  override def dataType: TypeReference = {
    val table = column.table
    val repository = model.modelRepository

    // Handle case (2) before case (1), because case (2) prevails when both are true, as described above.
    if (column.references.length == 1) {
      // Case (2): The column is a foreign key referencing exactly one unique key.
      repository.getProperty(column.references.head).dataType
    } else if (table.primaryKey == Seq(column)) {
      // Case (1): The column is the only primary key of its table.
      id(NamedTypeReference(model.fullName))
    } else {
      super.dataType
    }
  }
}
