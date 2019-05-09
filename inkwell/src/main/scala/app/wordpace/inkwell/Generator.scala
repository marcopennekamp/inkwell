package app.wordpace.inkwell

import java.nio.charset.StandardCharsets
import java.nio.file._

import app.wordpace.inkwell.generator.{CompilationUnit, CompilationUnitEmitter, ModelRepository}
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
        val repository = new ModelRepository(config)
        repository.addSchema(schema)
        val units = config.schemaSlicer.slice(repository)
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
    Paths.get(config.targetFolder.toString, unit.fullName.toFileName + ".scala")
  }

  /**
    * This emitter formats all compilation units either with Scalafmt or a code formatter of your choice
    * if you override this. Wraps the configured unit emitter.
    */
  protected def formattingUnitEmitter: CompilationUnitEmitter = {
    val scalafmt = Scalafmt.create(this.getClass.getClassLoader)
    val scalafmtConfig = config.scalafmtConfig.filter { p => // Ensure that the config file exists.
      val exists = Files.exists(p)
      if (!exists) println(s"Warning: Your scalafmt config file could not be found at $p.")
      exists
    }
    unit: CompilationUnit => {
      val code = config.unitEmitter(unit)
      // Only format the code with scalafmt if formatting is desired, hence the Option monad.
      scalafmtConfig.map(configPath => scalafmt.format(configPath, filePath(unit), code)).getOrElse(code)
    }
  }

  /**
    * Produces the [[Output]] value.
    */
  protected def produce(units: Set[CompilationUnit]): Output
}

/**
  * Simply returns the code for each [[CompilationUnit]].
  */
class StringGenerator(config: GeneratorConfiguration) extends DefaultGenerator[Set[(CompilationUnit, String)]](config) {
  override def produce(units: Set[CompilationUnit]): Set[(CompilationUnit, String)] = {
    units.map(unit => (unit, formattingUnitEmitter(unit)))
  }
}

/**
  * Writes each [[CompilationUnit]] to a file.
  */
class FileGenerator(config: GeneratorConfiguration) extends DefaultGenerator[Unit](config) {
  override def produce(units: Set[CompilationUnit]): Unit = {
    units.foreach { unit =>
      val code = formattingUnitEmitter(unit)
      val filePath = this.filePath(unit)
      Files.createDirectories(filePath.getParent)
      Files.write(filePath, code.getBytes(StandardCharsets.UTF_8))
    }
  }
}
