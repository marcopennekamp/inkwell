package com.github.choppythelumberjack.trivialgen.generator

import java.nio.charset.StandardCharsets
import java.nio.file._

import com.github.choppythelumberjack.trivialgen.WithFileNaming

case class GenerationException(message: String, cause: Throwable) extends RuntimeException(message, cause)

trait Generator extends WithFileNaming {
  def config: GeneratorConfiguration

  /**
    * Run the generator and write the output to file(s).
    */
  def generate(): Unit = {
    config.schemaReader.read().fold(
      ex => throw GenerationException("Couldn't read the schema due to an underlying exception.", ex),
      schema => {
        config.selectSchemaEmitter(schema).compilationUnits.foreach { unit =>
          val path = Paths.get(config.target.toString, unit.path.toString + ".scala")
          Files.createDirectories(path.getParent)
          Files.write(path, unit.code.getBytes(StandardCharsets.UTF_8))
        }
      }
    )
  }
}

class DefaultGenerator(override val config: GeneratorConfiguration) extends Generator
