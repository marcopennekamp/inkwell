package app.wordpace.inkwell.generator

import app.wordpace.inkwell.GeneratorConfiguration
import app.wordpace.inkwell.schema.{Column, Schema}

import scala.collection.mutable

/**
  * A central location to collect all [[Model]] objects.
  */
class ModelRepository(config: GeneratorConfiguration) {
  private val modelsMutable: mutable.Set[Model] = mutable.Set()

  /**
    * The models contained in the repository.
    */
  def models: Set[Model] = modelsMutable.toSet

  /**
    * Adds and builds the models from the schema.
    */
  def addSchema(schema: Schema): Unit = {
    val newModels = schema.tables.map(config.createModel)
    newModels.foreach(_.link(this))
    modelsMutable ++= newModels
  }

  /**
    * Finds the property associated with the given column.
    */
  def findProperty(column: Column): Option[Property] = modelsMutable.flatMap(_.properties).find(_.column == column)

  def getProperty(column: Column): Property = {
    findProperty(column).getOrElse(throw new RuntimeException(s"Couldn't find property for column $column"))
  }
}
