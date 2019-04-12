package com.github.choppythelumberjack.trivialgen.generator

import com.github.choppythelumberjack.trivialgen.generator.SchemaEmitter.CompilationUnit
import com.github.choppythelumberjack.trivialgen.schema._

/**
  * Handles the generation of the whole schema and has the power to decide in which files, objects or even
  * packages specific classes are placed.
  */
trait SchemaEmitter {
  /**
    * The schema to be transformed.
    */
  def schema: Schema

  /**
    * The generated compilation units which will be written to different files.
    */
  def compilationUnits: Seq[CompilationUnit]

  /**
    * The emitted header generated from the package declaration, import code and possibly additional code.
    */
  def header(unitName: String): String =
    s"""package ${packageName(unitName)}
       |
       |$importCode
       |
     """.stripMargin

  /**
    * The package name referenced in the unit's package declaration.
    */
  def packageName(unitName: String): String

  /**
    * The emitted import section below each unit's package declaration.
    */
  def importCode: String = imports.map {
    case e: Import.Entity => e.classTag.toString
    case p: Import.Package => s"${p.name}._"
  }.map(s => s"import $s").mkString("\n")

  /**
    * A set of imports.
    */
  def imports: Set[Import]
}

object SchemaEmitter {
  case class CompilationUnit(path: String, code: String)
}

/**
  * Generates the whole schema into a single file.
  */
class SingleFileSchemaEmitter(config: GeneratorConfiguration, override val schema: Schema) extends SchemaEmitter {
  override def compilationUnits: Seq[CompilationUnit] = {
    if (!config.target.isFile) {
      throw new RuntimeException("The SingleFileSchemaEmitter can only be used with a target file.")
    }

    val unitName = config.target.getName
    val tableCodes = schema.tables.map { table =>
      s"""${config.selectModelEmitter(table).code}
         |${config.selectCompanionEmitter(table).code}
         |
       """.stripMargin
    }
    val code = (header(unitName) + tableCodes).mkString("\n")
    Seq(CompilationUnit(unitName, code))
  }

  override def packageName(unitName: String): String = config.basePackage
  override def imports: Set[Import] = config.imports
}

// TODO: Idea: Partitioning schema emitter which emits models to different subpackages based on a predefined table.
