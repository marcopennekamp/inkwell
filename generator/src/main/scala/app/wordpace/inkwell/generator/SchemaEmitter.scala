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
       |$importCode""".stripMargin

  /**
    * The package name referenced in the unit's package declaration.
    */
  protected def packageName(unitName: String): String

  /**
    * The emitted import section below each unit's package declaration.
    */
  protected def importCode: String = imports.map {
    case e: Import.Entity => e.fullName
    case p: Import.Wildcard => s"${p.name}._"
  }.map(s => s"import $s").mkString("\n")

  /**
    * A set of imports.
    */
  protected def imports: Set[Import]
}

object SchemaEmitter {
  /**
    * @param name The name of the unit relative to the base package, e.g. `schema.Schema`.
    * @param code The unit's complete generated code.
    */
  case class CompilationUnit(name: String, code: String)
}

abstract class DefaultSchemaEmitter(config: GeneratorConfiguration, override val schema: Schema) extends SchemaEmitter {
  override def packageName(unitName: String): String = (config.basePackage + "." + unitName).cutLast('.')
  override def imports: Set[Import] = config.imports

  protected def tableCode(table: Table): String =
    s"""${config.selectModelEmitter(table).code}
       |${config.selectCompanionEmitter(table).code}""".stripMargin

  protected def toUnit(unitName: String, bodyCode: String): CompilationUnit = {
    CompilationUnit(unitName, header(unitName) + "\n\n" + bodyCode)
  }

  protected def toUnit(unitName: String, tables: Seq[Table]): CompilationUnit = {
    toUnit(unitName, tables.map(tableCode).mkString("\n\n"))
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
  * Partitions the schema into different packages based on a partitioning table. A single table may be included
  * in multiple partitions. Tables not included in any partition will be generated into the base package.
  *
  * @param simpleUnitName The simple name for every unit. For example, let's say we have a partition `(pack -> t1,
  *                 pack -> t2, back -> t3)`, tables t1, t2, t3, t4, and a unit name `Schema`. This schema
  *                 emitter will generate three units: `Schema` (t4), `pack.Schema` (t1, t2), and `back.Schema` (t3).
  */
class PartitioningSchemaEmitter(config: GeneratorConfiguration, schema: Schema, partitions: Map[String, Set[Table.Name]],
  simpleUnitName: String) extends DefaultSchemaEmitter(config, schema)
{
  override def compilationUnits: Seq[CompilationUnit] = {
    // Note that toVector ensures that the map is properly executed before we use the mutable unpartitioned set.
    val unpartitioned = mutable.Set(schema.tables: _*)
    val units = partitions.toSeq.map { case (partitionName, tableNames) =>
      val tables = schema.tables.filter(t => tableNames.contains(t.name))

      // Register the partitioned tables so they aren't generated into the unpartitioned unit.
      tables.foreach(unpartitioned.remove)

      // The name of the compilation unit contains the partition name, so that it's generated to the correct unit.
      val unitName = partitionName + "." + simpleUnitName
      toUnit(unitName, tables)
    }.toVector
    units ++ Seq(unpartitioned).filter(_.nonEmpty).map(tables => toUnit(simpleUnitName, tables.toSeq))
  }
}
