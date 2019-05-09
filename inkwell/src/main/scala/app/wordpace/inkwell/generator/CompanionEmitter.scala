package app.wordpace.inkwell.generator

import app.wordpace.inkwell.GeneratorConfiguration

trait CompanionEmitter {
  /**
    * Emits the code for the given model's companion object.
    */
  def apply(model: Model): String
}

// TODO: Add inheritance support for the companion object? It's useful in cases where pre-defined implicits
//       have to be associated with an object, or in general definitions. See also SchemaTests for an example
//       of a good use case with the age quote.

class DefaultCompanionEmitter(config: GeneratorConfiguration) extends CompanionEmitter {
  override def apply(model: Model): String = {
    val innerCode = this.innerCode(model)
    if (innerCode.isEmpty) {
      ""
    } else {
      s"""object ${model.simpleName} {
         |  $innerCode
         |}""".stripMargin
    }
  }

  /**
    * The inner code of the companion object.
    */
  protected def innerCode(model: Model): String = ""
}
