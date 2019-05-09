package app.wordpace.inkwell.generator

import app.wordpace.inkwell.GeneratorConfiguration
import app.wordpace.inkwell.schema.Table

/**
  * Represents a table during transformation to a case class.
  */
trait Model {
  /**
    * The table to be transformed.
    */
  def table: Table

  /**
    * The compilation unit that the model belongs to.
    */
  def compilationUnit: CompilationUnit

  /**
    * Links the given compilation unit to this model.
    *
    * Since [[CompilationUnit]] also expects a list of models, we have to defer the linking of this value,
    * as it's essentially an upwards-reference. Compare [[Table]].
    */
  def link(unit: CompilationUnit): Unit

  /**
    * The model repository that owns this model.
    */
  def modelRepository: ModelRepository

  /**
    * Links the given model repository to this model.
    */
  def link(repository: ModelRepository): Unit

  /**
    * The simple name of the model.
    */
  def simpleName: String

  /**
    * The full name of the model.
    */
  def fullName: String = TypeUtil.names.concat(compilationUnit.packageName, simpleName)

  /**
    * The properties of the model in correct order.
    */
  def properties: Seq[Property]

  /**
    * The supertypes of the model in correct order.
    */
  def supertypes: Seq[TypeReference]
}

case class DefaultModel(
  override val table: Table,
  config: GeneratorConfiguration,
  inheritances: Inheritances,
) extends Model {
  private var unit: CompilationUnit = _
  override def compilationUnit: CompilationUnit = unit
  override def link(unit: CompilationUnit): Unit = {
    assert(this.unit == null)
    this.unit = unit
  }

  private var repository: ModelRepository = _
  override def modelRepository: ModelRepository = repository
  override def link(repository: ModelRepository): Unit = {
    assert(this.repository == null)
    this.repository = repository
  }

  override def simpleName: String = config.namingStrategy.model(table)
  override def properties: Seq[Property] = table.columns.map(c => config.createProperty(c, this))
  override def supertypes: Seq[TypeReference] = inheritances.get(simpleName)
}
