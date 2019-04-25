package app.wordpace.inkwell.generator

import java.io.File
import java.nio.file.{Path, Paths}

import app.wordpace.inkwell.GeneratorConfiguration
import app.wordpace.inkwell.generator.SchemaEmitter.CompilationUnit
import app.wordpace.inkwell.schema._

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
    * This default implementation builds a file path from the package and name of the unit.
    *
    * @return An extensionless path to the file the unit should be written to.
    */
  protected def unitPath(unitName: String): Path = Paths.get(packageName(unitName).replace(".", File.pathSeparator), unitName)

  /**
    * The emitted header generated from the package declaration, import code and possibly additional code.
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
    * @param path An extensionless, absolute path to the file the unit should be written to.
    * @param code The unit's complete generated code.
    */
  case class CompilationUnit(path: Path, code: String)
}

/**
  * Generates the whole schema into a single file.
  */
class SingleFileSchemaEmitter(config: GeneratorConfiguration, override val schema: Schema) extends SchemaEmitter {
  override def compilationUnits: Seq[CompilationUnit] = {
    val tableCodes = schema.tables.map { table =>
      s"""${config.selectModelEmitter(table).code}
         |${config.selectCompanionEmitter(table).code}""".stripMargin
    }
    val unitName = config.target.getFileName.toString
    // Each "" adds a blank line. Thus, we add a blank line between the header and the main code and also between tables.
    val code = (header(unitName) +: "" +: tableCodes.flatMap(s => Seq(s, ""))).mkString("\n")
    Seq(CompilationUnit(config.target, code))
  }

  override def packageName(unitName: String): String = config.basePackage
  override def imports: Set[Import] = config.imports
}

// TODO: Idea: Partitioning schema emitter which emits models to different subpackages based on a predefined table.
