package app.wordpace.inkwell.generator

import app.wordpace.inkwell.GeneratorConfiguration

trait ModelEmitter {
  /**
    * Emits the code for the given model.
    */
  def apply(model: Model): String
}

class DefaultModelEmitter(config: GeneratorConfiguration) extends ModelEmitter {
  override def apply(model: Model): String = {
    s"case class ${model.simpleName}(${emitProperties(model).mkString(", ")})${extendsClause(model)}"
  }

  /**
    * The emitted extends clause of the case class declaration.
    */
  protected def extendsClause(model: Model): String = {
    val supertypes = emitSupertypes(model)
    if (supertypes.nonEmpty) {
      (s" extends ${supertypes.head}" :: supertypes.tail.toList).mkString(" with ")
    } else {
      ""
    }
  }

  /**
    * The (emitted) properties of the case class.
    */
  protected def emitProperties(model: Model): Seq[String] = model.properties.map(p => config.propertyEmitter(p))

  /**
    * The (emitted) supertypes of the case class.
    */
  protected def emitSupertypes(model: Model): Seq[String] = {
    model.supertypes.map(t => config.typeEmitter.apply(t)(model.compilationUnit))
  }
}
