package app.wordpace.inkwell.generator

import app.wordpace.inkwell.util._

/**
  * A single unit of compilation, such as a file. Each compilation unit may contain many models.
  */
trait CompilationUnit {
  /**
    * The full name of the unit including the base package, e.g. `base.schema.Schema`.
    */
  def fullName: String

  /**
    * The package name of the unit, e.g. `base.schema`.
    */
  def packageName: String = fullName.cutPackageName

  /**
    * The models contained in the unit.
    */
  def models: Set[Model]

  /**
    * The set of imports.
    */
  def imports: Set[Import]
}

case class DefaultCompilationUnit(
  override val fullName: String,
  override val models: Set[Model],
  override val imports: Set[Import],
) extends CompilationUnit {
  // Link the compilation unit to each model.
  models.foreach(_.link(this))
}
