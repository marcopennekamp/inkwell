package com.github.choppythelumberjack.trivialgen

import java.nio.charset.StandardCharsets
import java.nio.file._

import org.scalafmt.interfaces.Scalafmt

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
        val scalafmt = Scalafmt.create(this.getClass.getClassLoader)
        val scalafmtConfig = Paths.get("generator/src/main/resources/scalafmt.conf")
        //val scalafmtConfig = Paths.get(ClassLoader.getSystemResource("/scalafmt.conf").toURI)
        config.selectSchemaEmitter(schema).compilationUnits.foreach { unit =>
          val path = Paths.get(unit.path.toString + ".scala")
          val code = scalafmt.format(scalafmtConfig, path, unit.code)
          Files.createDirectories(path.getParent)
          Files.write(path, code.getBytes(StandardCharsets.UTF_8))
        }
      }
    )
  }
}

class DefaultGenerator(override val config: GeneratorConfiguration) extends Generator
