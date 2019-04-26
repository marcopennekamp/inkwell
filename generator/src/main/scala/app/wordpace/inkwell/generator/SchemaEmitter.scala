package app.wordpace.inkwell.generator

import java.io.File
import java.nio.file.{Path, Paths}

import app.wordpace.inkwell.GeneratorConfiguration
import app.wordpace.inkwell.generator.SchemaEmitter.CompilationUnit
import app.wordpace.inkwell.schema._

import app.wordpace.inkwell.util.StringExtensions

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

  protected def unitCode(unitName: String, body: String): String = header(unitName) + "\n\n" + body
  protected def unitCode(unitName: String, tables: Seq[Table]): String = unitCode(unitName, tables.map(tableCode).mkString("\n\n"))
}

/**
  * Generates the whole schema into a single file.
  */
class SingleFileSchemaEmitter(config: GeneratorConfiguration, schema: Schema, unitName: String)
  extends DefaultSchemaEmitter(config, schema)
{
  override def compilationUnits: Seq[CompilationUnit] = {
    Seq(CompilationUnit(unitName, unitCode(unitName, schema.tables)))
  }

}

// TODO: Idea: Partitioning schema emitter which emits models to different subpackages based on a predefined table.
