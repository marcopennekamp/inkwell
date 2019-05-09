package app.wordpace.inkwell.generator

import app.wordpace.inkwell.GeneratorConfiguration

import app.wordpace.inkwell.util._

trait CompilationUnitEmitter {
  /**
    * Emits the code for the given compilation unit.
    */
  def apply(unit: CompilationUnit): String
}

class DefaultCompilationUnitEmitter(config: GeneratorConfiguration) extends CompilationUnitEmitter {
  override def apply(unit: CompilationUnit): String = {
    val body = unit.models.map(modelCode).mkString("\n\n")
    header(unit) + "\n\n" + body
  }

  /**
    * The combined model code for the companion and case class.
    */
  protected def modelCode(model: Model): String = {
    Seq(config.modelEmitter(model), config.companionEmitter(model)).flatMap(_.emptyToNone).mkString("\n")
  }

  /**
    * The emitted header generated from the package declaration, imports and possibly additional code.
    */
  protected def header(unit: CompilationUnit): String = {
    Seq(
      s"package ${unit.packageName}",
      importCode(unit),
    ).flatMap(_.emptyToNone).mkString("\n\n")
  }

  /**
    * The emitted import section below the unit's package declaration.
    */
  protected def importCode(unit: CompilationUnit): String = unit.imports.map {
    case e: Import.Entity => e.fullName
    case p: Import.Wildcard => s"${p.name}._"
  }.map(s => s"import $s").mkString("\n")

  // TODO: Sort imports alphabetically for cleaner files.
}
