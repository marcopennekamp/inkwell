package app.wordpace.inkwell.generator

import app.wordpace.inkwell.GeneratorConfiguration

trait PropertyEmitter {
  /**
    * Emits the code for the given property.
    */
  def apply(property: Property): String
}

class DefaultPropertyEmitter(config: GeneratorConfiguration) extends PropertyEmitter {
  override def apply(property: Property): String = {
    s"${property.name}: ${tpe(property)}${property.defaultValue.map(v => s" = $v").getOrElse("")}"
  }

  /**
    * The stringification of [[Property.tpe]].
    */
  protected def tpe(property: Property): String = config.typeEmitter(property.tpe)(property.model.compilationUnit)
}
