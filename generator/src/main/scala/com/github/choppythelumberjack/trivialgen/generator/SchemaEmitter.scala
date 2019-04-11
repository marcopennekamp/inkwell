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

  def compilationUnits: Seq[CompilationUnit]

}

object SchemaEmitter {
  case class CompilationUnit(path: String, code: String)
}
