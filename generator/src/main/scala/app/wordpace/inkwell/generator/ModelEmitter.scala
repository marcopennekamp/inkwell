package app.wordpace.inkwell.generator

import app.wordpace.inkwell.GeneratorConfiguration
import app.wordpace.inkwell.schema.Table

/**
  * Handles the transformation of one table to a (case) class.
  */
trait ModelEmitter {
  /**
    * The table to be transformed.
    */
  protected def table: Table

  /**
    * The generated code for the case class. Please ensure that the name is consistent with the naming strategy.
    */
  def code: String = s"case class ${table.scalaName}(${properties.mkString(", ")}) $extendsClause"

  /**
    * The emitted extends clause of the case class declaration.
    */
  protected def extendsClause: String = {
    if (supertypes.nonEmpty) {
      (s"extends ${supertypes.head}" :: supertypes.tail.toList).mkString(" with ")
    } else {
      ""
    }
  }

  /**
    * The naming strategy for the model name.
    */
  implicit def namingStrategy: NamingStrategy

  /**
    * The (emitted) properties of the case class.
    */
  protected def properties: Seq[String]

  /**
    * The (emitted) supertypes of the case class.
    */
  protected def supertypes: Seq[String]
}

/**
  * Generates a simple case class based on the configured naming strategy, selected property emitter
  * and inheritance configurations.
  */
class DefaultModelEmitter(
  config: GeneratorConfiguration,
  inheritances: Inheritances,
  override val table: Table
) extends ModelEmitter {
  override implicit def namingStrategy: NamingStrategy = config.namingStrategy
  override def properties: Seq[String] = table.columns.map(c => config.selectPropertyEmitter(c).code)
  override def supertypes: Seq[String] = inheritances.get(table.scalaName).map(config.typeEmitter.apply)
}
