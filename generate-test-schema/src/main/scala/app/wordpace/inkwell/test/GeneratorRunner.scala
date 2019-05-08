package app.wordpace.inkwell.test

import java.nio.file.{Path, Paths}
import java.time.LocalDateTime

import app.wordpace.inkwell.generator._
import app.wordpace.inkwell.schema.Schema
import app.wordpace.inkwell.{DefaultGeneratorConfiguration, FileGenerator, GeneratorConfiguration}

import scala.reflect.runtime.universe.typeOf

/**
  * Generates source files for the test-generated-code project.
  */
object GeneratorRunner {

  // TODO: Test multiple trait inheritance.
  // TODO: Test companion object code generation.
  // TODO: Test custom types.
  // TODO: Test "upwards" references in schema model (column -> table and table -> schema).
  // TODO: Test how KeyAsIdColumnPlugin behaves with multi-line primary and foreign keys.

  def main(args: Array[String]): Unit = {
    val path = args(0)
    plumbusAcademy(path)
    food(path)
  }

  /**
    * This configuration tests: Code generation, nullable columns to Options, basic imports, single trait inheritance,
    * KeyAsIdColumnPlugin and the foreign/primary key model.
    */
  def plumbusAcademy(targetFolder: String): Unit = {
    import TypeReference.conversions._

    val config: DefaultGeneratorConfiguration = new DefaultGeneratorConfiguration(
      ConfigLoader.databaseConfiguration("plumbus_academy.sql"),
      sourceSchema = "PUBLIC",
      targetFolder = Paths.get(targetFolder),
      basePackage = "plumbus.academy",
    ) { configSelf =>
      override val imports: Set[Import] = Set(
        Import.Wildcard("plumbus.academy"), // Testing package imports.
        Import.Entity(typeOf[LocalDateTime]), // Testing typeOf imports.
        Import.Entity("java.nio.file.Paths"), // Testing fullName imports.
        Import.Entity("core.Id"),
      )

      override def inheritances: Inheritances = Inheritances(Map(
        "Person" -> Seq(
          // TODO: Test referring to a trait via typeOf.
          "plumbus.academy.PersonFunctions",
        ),
      ))

      override def scalafmtConfig: Option[Path] = Some(Paths.get("generator", "src", "test", "resources", "scalafmt.conf"))

      override lazy val typeEmitter: TypeEmitter = new ImportSimplifyingTypeEmitter(imports) with KeyAsIdColumnPlugin {
        override protected def config: GeneratorConfiguration = configSelf
      }
    }
    new FileGenerator(config).generate()
  }

  /**
    * This configuration tests: Partitioned schemas.
    */
  def food(targetFolder: String): Unit = {
    val config: DefaultGeneratorConfiguration = new DefaultGeneratorConfiguration(
      ConfigLoader.databaseConfiguration("food.sql"),
      sourceSchema = "PUBLIC",
      targetFolder = Paths.get(targetFolder),
      basePackage = "food",
    ) { configSelf =>
      override val imports: Set[Import] = Set(
        // Make sure that regular imports are still resolved even when partitions are imported.
        Import.Entity(typeOf[LocalDateTime]),
        Import.Entity("core.Id"),
      )

      override def scalafmtConfig: Option[Path] = Some(Paths.get("generator", "src", "test", "resources", "scalafmt.conf"))

      // We need the KeyAsId plugin, because we want to test that references to other partitions are resolved
      // correctly.
      override lazy val typeEmitter: TypeEmitter = new ImportSimplifyingTypeEmitter(imports) with KeyAsIdColumnPlugin {
        override protected def config: GeneratorConfiguration = configSelf
      }

      override def selectSchemaEmitter(schema: Schema): SchemaEmitter = {
        // We don't partition 'knife' by design.
        val partitions: Map[String, Set[String]] = Map(
          "fruit" ->  Set("Apple", "Pear", "Orange"),
          "dough" -> Set("Bread", "Pizza"),
        )
        new PartitioningSchemaEmitter(this, schema, partitions, "Schema")
      }
    }
    new FileGenerator(config).generate()
  }

}
