package app.wordpace.inkwell.generator

import app.wordpace.inkwell.GeneratorConfiguration
import app.wordpace.inkwell.generator.SchemaEmitter.CompilationUnit
import app.wordpace.inkwell.schema._
import app.wordpace.inkwell.util.StringExtensions

import scala.collection.mutable

/**
  * Handles the generation of the whole schema and has the power to decide in which files, objects or even
  * packages specific classes are placed.
  */
trait SchemaEmitter {
  /**
    * The schema to be transformed.
    */
  protected def schema: Schema

  /**
    * The generated compilation units which will be written to different files.
    */
  def compilationUnits: Seq[CompilationUnit]

  /**
    * The emitted header generated from the package declaration, imports and possibly additional code.
    */
  protected def header(unitName: String): String =
    s"""package ${packageName(unitName)}
       |
       |${importCode(unitName)}""".stripMargin

  /**
    * The package name referenced in the unit's package declaration.
    */
  protected def packageName(unitName: String): String

  /**
    * The emitted import section below each unit's package declaration.
    */
  protected def importCode(unitName: String): String = imports(unitName).map {
    case e: Import.Entity => e.fullName
    case p: Import.Wildcard => s"${p.name}._"
  }.map(s => s"import $s").mkString("\n")

  /**
    * A set of imports.
    */
  protected def imports(unitName: String): Set[Import]
}

object SchemaEmitter {
  /**
    * @param name The name of the unit relative to the base package, e.g. `schema.Schema`.
    * @param code The unit's complete generated code.
    */
  case class CompilationUnit(name: String, code: String)
}

abstract class DefaultSchemaEmitter(config: GeneratorConfiguration, override val schema: Schema) extends SchemaEmitter {
  override def packageName(unitName: String): String = TypeUtil.names.concat(config.basePackage, unitName).cutLast('.')
  override def imports(unitName: String): Set[Import] = config.imports

  /**
    * Emits code for the given table.
    */
  protected def tableCode(table: Table): String =
    s"""${config.selectModelEmitter(table).code}
       |${config.selectCompanionEmitter(table).code}""".stripMargin

  /**
    * Turns a unit name and code body (without the header) into a full compilation unit.
    */
  protected def toUnit(unitName: String, bodyCode: String): CompilationUnit = {
    CompilationUnit(unitName, header(unitName) + "\n\n" + bodyCode)
  }

  /**
    * Turns a unit name and a sequence of tables into a compilation unit.
    */
  protected def toUnit(unitName: String, tables: Seq[Table]): CompilationUnit = {
    toUnit(unitName, tables.sortBy(_.scalaName(config.namingStrategy)).map(tableCode).mkString("\n\n"))
  }
}

/**
  * Generates the whole schema into a single file.
  */
class SingleFileSchemaEmitter(config: GeneratorConfiguration, schema: Schema, unitName: String)
  extends DefaultSchemaEmitter(config, schema)
{
  override def compilationUnits: Seq[CompilationUnit] = Seq(toUnit(unitName, schema.tables))
}

/**
  * Partitions the schema into different packages based on a partitioning map. A single model may be included
  * in multiple partitions. Models not included in any partition will be generated into the base package.
  *
  * @param partitions The map of partitions from a partition name to a set of <b>scala names.</b> We, again,
  *                   believe Scala names are more apt to represent each table, because we are ultimately
  *                   partitioning the space of Scala types, not the space of database tables.
  * @param simpleUnitName The simple name for every unit. For example, let's say we have a partition
  *                       `(pack -> c1, pack -> c2, back -> c3)`, class names c1, c2, c3, c4, and a unit
  *                       name `Schema`. This schema emitter will generate three units: `Schema` (c4),
  *                       `pack.Schema` (c1, c2), and `back.Schema` (c3).
  */
class PartitioningSchemaEmitter(config: GeneratorConfiguration, schema: Schema, partitions: Map[String, Set[String]],
  simpleUnitName: String) extends DefaultSchemaEmitter(config, schema)
{
  override def compilationUnits: Seq[CompilationUnit] = {
    // Note that toVector ensures that the map is properly executed before we use the mutable unpartitioned set.
    val unpartitioned = mutable.Set(schema.tables: _*)
    val units = partitions.toSeq.map { case (partitionName, classNames) =>
      val tables = schema.tables.filter(t => classNames.contains(t.scalaName(config.namingStrategy)))

      // Register the partitioned tables so they aren't generated into the unpartitioned unit.
      tables.foreach(unpartitioned.remove)

      // The name of the compilation unit contains the partition name, so that it's generated to the correct unit.
      val unitName = TypeUtil.names.concat(partitionName, simpleUnitName)
      toUnit(unitName, tables)
    }.toVector
    units ++ Seq(unpartitioned).filter(_.nonEmpty).map(tables => toUnit(simpleUnitName, tables.toSeq))
  }

  /**
    * Import all other partitions (potentially including the unpartitioned package).
    */
  override def imports(unitName: String): Set[Import] = {
    val names = partitions.keys ++ Seq("") // Also (potentially) import unpartitioned units.
    val partitionImports = names.flatMap { name =>
      // This is the package that is already "imported", since the package a unit belongs to doesn't need
      // to be imported.
      val unitPackage = packageName(unitName)

      // This is the package of the partition to be imported.
      val partitionPackage = TypeUtil.names.concat(config.basePackage, name)

      if (partitionPackage == unitPackage) None else Some(partitionPackage)
    }.map(name => Import.Wildcard(name))
    super.imports(unitName) ++ partitionImports
  }
}
