package com.github.choppythelumberjack.trivialgen

import java.nio.charset.StandardCharsets
import java.nio.file._
import java.sql.{Connection, DriverManager}

import com.github.choppythelumberjack.trivialgen.GeneratorHelpers.indent
import com.github.choppythelumberjack.trivialgen.ScalaLangUtil.escape
import com.github.choppythelumberjack.trivialgen.ext.DatabaseTypes.DatabaseType
import com.github.choppythelumberjack.trivialgen.generator.{StereotypePackager, _}
import com.github.choppythelumberjack.trivialgen.model.StereotypingService.Namespacer
import com.github.choppythelumberjack.trivialgen.model._
import com.github.choppythelumberjack.trivialgen.schema.{DatabaseType, DefaultSchemaNameResolver, DefaultSchemaReader, JdbcTableMeta, SchemaNameResolver, SchemaReader}
import com.github.choppythelumberjack.trivialgen.util.StringUtil._
import com.github.choppythelumberjack.trivialgen.util.StringSeqUtil._

case class GenerationException(message: String, cause: Throwable) extends RuntimeException(message, cause)

trait SingleGeneratorFactory[+G] extends ((EmitterSettings) => G)

trait Generator extends WithFileNaming { self: CodeGeneratorComponents =>

  // Primarily to be used in child implementations in order to be able to reference SingleUnitCodegen
  // or some subclass of it that the child implementation will have. See how the ComposeableTraitsGen
  // uses this in the packaging strategy.
  override type Gen <: CodeEmitter

  def config: CodeGeneratorConfig
  def db: Connection

  def packagePrefix: String

  val namespacer: Namespacer = new DefaultNamespacer(schemaGetter)

  def generatorMaker = new SingleGeneratorFactory[CodeEmitter] {
    override def apply(emitterSettings: EmitterSettings): CodeEmitter =
      new CodeEmitter(emitterSettings)
  }

  class MultiGeneratorFactory[G](someGenMaker:SingleGeneratorFactory[G]) {
    def apply: Seq[G] = {
      schemaReader.read().fold(
        ex => throw GenerationException("Couldn't read the schema due to an underlying exception.", ex),
        schema => {
          ()
        }
      )

      // combine the generated elements as dictated by the packaging strategy and write the generator
      val configsAndGenerators =
        schemas.flatMap({ case (conf, schemas) =>
          val genProcess = new StereotypePackager[G]
          genProcess.packageIntoEmitters(someGenMaker, conf, packagingStrategy, stereotype(schemas))
        })

      configsAndGenerators
    }
  }
  def makeGenerators = new MultiGeneratorFactory[CodeEmitter](generatorMaker).apply

  def generate(): Unit = {
    // can't put Seq[Gen] into here because doing Seq[Gen] <: SingleUnitCodegen makes it covariant
    // and args here needs to be contravariant
    def makeGenWithCorrespondingFile(gens:Seq[CodeEmitter]) = {
      type Method = (CodeEmitter) => Path

      gens.map(gen => {
        def DEFAULT_NAME = gen.defaultNamespace

        def tableName =
          gen.caseClassTables.headOption
            .orElse(gen.querySchemaTables.headOption)
            .map(_.table.name)
            .getOrElse(DEFAULT_NAME)

        val fileName: Path =
          (packagingStrategy.fileNamingStrategy, gen.codeWrapper) match {
            case (ByPackageObjectStandardName, _) =>
              Paths.get("package")

            // When the user wants to group tables by package, and use a standard package heading,
            // create a new package with the same name. For example say you have a
            // public.Person table (in schema.table notation) if a namespacer that
            // returns 'public' is used. The resulting file will be public/PublicExtensions.scala
            // which will have a 'Public' table definition
            case (ByPackageName, PackageHeader(packageName)) =>
              Paths.get(packageName, packageName)

            case (ByPackageName, _) =>
              Paths.get(gen.packageName.getOrElse(DEFAULT_NAME))

            case (ByTable, PackageHeader(packageName)) =>
              Paths.get(packageName, tableName)

            // First case classes table name or first Query Schemas table name, or default if both empty
            case (ByTable, _) =>
              Paths.get(gen.packageName.getOrElse(DEFAULT_NAME), tableName)

            case (ByDefaultName, _) =>
              Paths.get(DEFAULT_NAME)

            // Looks like 'Method' needs to be explicitly here since it doesn't understand Gen type annotation is actually SingleUnitCodegen
            case (BySomeTableData(method:Method), _) =>
              method(gen)
          }

        val fileWithExtension = fileName.resolveSibling(fileName.getFileName + ".scala")
        val loc = Paths.get(config.targetFolder)

        (gen, Paths.get(config.targetFolder, fileWithExtension.toString))
      })
    }

    val generatorsAndFiles = makeGenWithCorrespondingFile(makeGenerators)

    generatorsAndFiles.foreach({ case (gen, filePath) => {
        Files.createDirectories(filePath.getParent)
        val content = gen.apply
        Files.write(filePath, content.getBytes(StandardCharsets.UTF_8))
      }
    })
  }

  val renderMembers = nameParser match {
    case CustomNames(_, _) => true
    case _ => false
  }

  /**
    * Run the Generator and return objects as strings
    *
    * @return
    */
  def writeStrings = makeGenerators.map(_.apply)

}
