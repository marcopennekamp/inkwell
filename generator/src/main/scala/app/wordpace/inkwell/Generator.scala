package app.wordpace.inkwell

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
        val scalafmtConfig = config.scalafmtConfig.filter { p => // Ensure that the config file exists.
          val exists = Files.exists(p)
          if (!exists) println(s"Warning: Your scalafmt config file could not be found at $p.")
          exists
        }
        config.selectSchemaEmitter(schema).compilationUnits.foreach { unit =>
          val filePath = Paths.get(unit.path.toString + ".scala")
          // Only format the code with scalafmt if formatting is desired.
          val code = scalafmtConfig.map(path => scalafmt.format(path, filePath, unit.code)).getOrElse(unit.code)
          Files.createDirectories(filePath.getParent)
          Files.write(filePath, code.getBytes(StandardCharsets.UTF_8))
        }
      }
    )
  }
}

class DefaultGenerator(override val config: GeneratorConfiguration) extends Generator
