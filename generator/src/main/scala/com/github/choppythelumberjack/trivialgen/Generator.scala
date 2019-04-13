package com.github.choppythelumberjack.trivialgen

import java.nio.charset.StandardCharsets
import java.nio.file._

case class GenerationException(message: String, cause: Throwable) extends RuntimeException(message, cause)

trait Generator {
  def config: GeneratorConfiguration

  /**
    * Run the generator and write the output to file(s).
    */
  def generate(): Unit = {
    config.schemaReader.read().fold(
      ex => throw GenerationException("Couldn't read the schema due to an underlying exception.", ex),
      schema => {
        config.selectSchemaEmitter(schema).compilationUnits.foreach { unit =>
          // TODO: Use scalafmt to format the generated code.
          val path = Paths.get(unit.path.toString + ".scala")
          Files.createDirectories(path.getParent)
          Files.write(path, unit.code.getBytes(StandardCharsets.UTF_8))
        }
      }
    )
  }
}

class DefaultGenerator(override val config: GeneratorConfiguration) extends Generator
