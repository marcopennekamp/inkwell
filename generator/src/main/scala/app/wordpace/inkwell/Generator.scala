package app.wordpace.inkwell

import java.nio.charset.StandardCharsets
import java.nio.file._

import app.wordpace.inkwell.generator.SchemaEmitter.CompilationUnit
import app.wordpace.inkwell.util.StringExtensions
import org.scalafmt.interfaces.Scalafmt

case class GenerationException(message: String, cause: Throwable) extends RuntimeException(message, cause)

trait Generator[Output] {
  /**
    * Run the generator and produce an [[Output]] value.
    */
  def generate(): Output
}

abstract class DefaultGenerator[Output](val config: GeneratorConfiguration) extends Generator[Output] {
  override def generate(): Output = {
    config.schemaReader.read().fold(
      ex => throw GenerationException("Couldn't read the schema due to an underlying exception.", ex),
      schema => {
        val units = formatCode(config.selectSchemaEmitter(schema).compilationUnits)
        produce(units)
      }
    )
  }

  /**
    * Create a file path from the given compilation unit.
    *
    * From a design perspective, this shouldn't actually be part of [[DefaultGenerator]] but rather
    * [[FileGenerator]]. However, [[Scalafmt]] requires the file path of the generated "file".
    */
  protected def filePath(unit: CompilationUnit): Path = {
    Paths.get(config.targetFolder.toString, config.basePackage.toFileName, unit.name.toFileName + ".scala")
  }

  /**
    * Formats all compilation units either with Scalafmt or a code formatter of your choice if you override this.
    */
  protected def formatCode(units: Seq[CompilationUnit]): Seq[CompilationUnit] = {
    val scalafmt = Scalafmt.create(this.getClass.getClassLoader)
    val scalafmtConfig = config.scalafmtConfig.filter { p => // Ensure that the config file exists.
      val exists = Files.exists(p)
      if (!exists) println(s"Warning: Your scalafmt config file could not be found at $p.")
      exists
    }
    units.map { unit =>
      // Only format the code with scalafmt if formatting is desired, hence the Option monad.
      val code = scalafmtConfig.map(configPath => scalafmt.format(configPath, filePath(unit), unit.code)).getOrElse(unit.code)
      CompilationUnit(unit.name, code)
    }
  }

  /**
    * Produces the [[Output]] value.
    */
  protected def produce(units: Seq[CompilationUnit]): Output
}

/**
  * Simply returns each [[CompilationUnit]].
  */
class StringGenerator(config: GeneratorConfiguration) extends DefaultGenerator[Seq[CompilationUnit]](config) {
  override def produce(units: Seq[CompilationUnit]): Seq[CompilationUnit] = units
}

/**
  * Writes each [[CompilationUnit]] to a file.
  */
class FileGenerator(config: GeneratorConfiguration) extends DefaultGenerator[Unit](config) {
  override def produce(units: Seq[CompilationUnit]): Unit = {
    units.foreach { unit =>
      val filePath = this.filePath(unit)
      Files.createDirectories(filePath.getParent)
      Files.write(filePath, unit.code.getBytes(StandardCharsets.UTF_8))
    }
  }
}
