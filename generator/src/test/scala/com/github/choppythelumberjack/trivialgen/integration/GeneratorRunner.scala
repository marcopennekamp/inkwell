package com.github.choppythelumberjack.trivialgen.integration

import java.nio.file.Paths

import com.github.choppythelumberjack.trivialgen.generator.{Import, SchemaInheritances, TableInheritances}
import com.github.choppythelumberjack.trivialgen.{DefaultGenerator, DefaultGeneratorConfiguration}

/**
  * Generates source files for the integration-tests project.
  */
object GeneratorRunner {

  // TODO: Add Option type test.
  // TODO: Add entity imports test.
  // TODO: Add multiple trait inheritance test.
  // TODO: Add companion object code generation test.
  // TODO: Add enum types test.
  // TODO: Add custom raw type test combined with foreign key and primary key test (such as an Id[T] type).
  //       This one could also function as an example for how to implement ID types.

  def main(args: Array[String]): Unit = {
    val path = args(0)
    plumbusAcademy(path)
  }

  /**
    * This configuration tests: Code generation, basic package imports, single trait inheritance.
    */
  def plumbusAcademy(basePath: String): Unit = {
    val config: DefaultGeneratorConfiguration = new DefaultGeneratorConfiguration(
      ConfigLoader.databaseConfiguration("plumbus_academy.sql"),
      sourceSchema = "PUBLIC",
      target = Paths.get(basePath, "plumbus", "academy", "Schema"),
      basePackage = "plumbus.academy",
    ) {
      override val imports: Set[Import] = Set(
        Import.Package("plumbus.academy"),
      )

      override def inheritances: SchemaInheritances = SchemaInheritances(Map(
        "Person" -> TableInheritances(
          fullNames = Seq("plumbus.academy.PersonFunctions"),
        ),
      ))
    }
    new DefaultGenerator(config).generate()
  }

}
