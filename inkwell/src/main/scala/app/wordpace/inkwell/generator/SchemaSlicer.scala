package app.wordpace.inkwell.generator

import app.wordpace.inkwell.GeneratorConfiguration
import app.wordpace.inkwell.util._

import scala.collection.mutable

/**
  * Distributes [[Model]] objects via [[CompilationUnit]]. Each model may only be distributed to a single
  * compilation unit.
  *
  * Handles the distribution of the whole schema and has the power to decide in which packages specific
  * classes are placed.
  */
trait SchemaSlicer {
  /**
    * Slices the [[ModelRepository]] into a set of [[CompilationUnit]].
    */
  def slice(repository: ModelRepository): Set[CompilationUnit]
}

abstract class DefaultSchemaSlicer(config: GeneratorConfiguration) extends SchemaSlicer {
  /**
    * Creates the actual [[CompilationUnit]]. Override this if you want to use a different compilation unit.
    * Creates a [[DefaultCompilationUnit]] by default.
    */
  protected def toUnit(fullName: String, models: Set[Model], imports: Set[Import]): CompilationUnit = {
    DefaultCompilationUnit(fullName, models, imports)
  }
}

/**
  * Generates the whole schema into a single unit.
  */
class SingleUnitSchemaSlicer(
  config: GeneratorConfiguration,
  unitName: String
) extends DefaultSchemaSlicer(config) {
  override def slice(repository: ModelRepository): Set[CompilationUnit] = {
    Set(toUnit(TypeUtil.names.concat(config.basePackage, unitName), repository.models, config.imports))
  }
}

/**
  * Partitions the schema into different packages based on a partitioning map. A single model may only be
  * included in one partition. Models not included in any partition will be generated into the base package.
  *
  * @param partitions The map of partitions from a partition name to a set of <b>scala names.</b> We believe
  *                   Scala names are more apt to represent each table, because we are ultimately partitioning
  *                   the space of Scala types, not the space of database tables.
  * @param simpleUnitName The simple name for every unit. For example, let's say we have a partition
  *                       `(pack -> c1, pack -> c2, back -> c3)`, class names c1, c2, c3, c4, and a unit
  *                       name `Schema`. This schema slicer will generate three units: `Schema` (c4),
  *                       `pack.Schema` (c1, c2), and `back.Schema` (c3).
  */
class PartitioningSchemaSlicer(
  config: GeneratorConfiguration,
  partitions: Map[String, Set[String]],
  simpleUnitName: String
) extends DefaultSchemaSlicer(config) {
  override def slice(repository: ModelRepository): Set[CompilationUnit] = {
    // Note that toSet also ensures that the map is properly executed before we use the mutable unpartitioned set.
    val unpartitioned = mutable.Set(repository.models.toSeq: _*)
    val units = partitions.toSeq.map { case (partitionName, classNames) =>
      val partitionModels = unpartitioned.filter(m => classNames.contains(m.simpleName)).toSet

      // Register the partitioned tables so they aren't generated into the unpartitioned unit.
      partitionModels.foreach(unpartitioned.remove)

      // The name of the compilation unit contains the partition name, so that it's generated to the correct unit.
      val unitFullName = TypeUtil.names.concat(config.basePackage, partitionName, simpleUnitName)

      toUnit(unitFullName, partitionModels, imports(unitFullName))
    }.toSet

    val unpartitionedFullName = TypeUtil.names.concat(config.basePackage, simpleUnitName)
    units ++ Seq(unpartitioned.toSet).filter(_.nonEmpty).map { tables =>
      toUnit(unpartitionedFullName, tables, imports(unpartitionedFullName))
    }
  }

  /**
    * Import all other partitions (potentially including the unpartitioned package).
    */
  def imports(unitFullName: String): Set[Import] = {
    // TODO: Use access to the models to check which packages need to be imported exactly.
    val names = partitions.keys ++ Seq("") // Also (potentially) import unpartitioned units.
    val partitionImports = names.flatMap { name =>
      // This is the package that is already "imported", since the package a unit belongs to doesn't need
      // to be imported.
      val unitPackage = unitFullName.cutPackageName

      // This is the package of the partition to be imported.
      val partitionPackage = TypeUtil.names.concat(config.basePackage, name)

      if (partitionPackage == unitPackage) None else Some(partitionPackage)
    }.map(name => Import.Wildcard(name))
    config.imports ++ partitionImports
  }
}
