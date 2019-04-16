package com.github.choppythelumberjack.trivialgen.integration

import java.nio.file.Path

import com.github.choppythelumberjack.trivialgen.{DatabaseConfiguration, DefaultGeneratorConfiguration}

object ConfigLoader {
  val defaultSchemaName = "PUBLIC"

  def databaseConfiguration(sqlScriptName: String): DatabaseConfiguration = {
    DatabaseConfiguration(s"jdbc:h2:mem:sample;INIT=RUNSCRIPT FROM 'generator/src/test/resources/$sqlScriptName'", "sa", "")
  }

  def singleFileConfig(sqlScriptName: String, target: Path, basePackage: String) = DefaultGeneratorConfiguration(
    databaseConfiguration(sqlScriptName),
    defaultSchemaName,
    target,
    basePackage,
  )
}
