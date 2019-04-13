package com.github.choppythelumberjack.trivialgen.integration

import java.nio.file.Paths

import com.github.choppythelumberjack.trivialgen.DefaultGenerator

/**
  * Generates source files for the integration-tests project.
  */
object GeneratorRunner {

  // TODO: Add imports test.
  // TODO: Add inheritance test.
  // TODO: Add companion object code generation test.
  // TODO: Add enum types test.
  // TODO: Add custom raw type test combined with foreign key and primary key test (such as an Id[T] type).
  //       This one could also function as an example for how to implement ID types.

  def main(args: Array[String]): Unit = {
    val path = args(0)
    defaultGenerator(path)
  }

  def defaultGenerator(basePath: String): Unit = {
    val plumbusAcademy = new DefaultGenerator(ConfigLoader.singleFileConfig(
      "plumbus_academy.sql", Paths.get(basePath, "plumbus", "academy", "Schema"), "plumbus.academy"
    ))
    plumbusAcademy.generate()
  }

}
