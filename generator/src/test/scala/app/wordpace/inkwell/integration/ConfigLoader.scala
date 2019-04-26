package app.wordpace.inkwell.integration

import java.nio.file.Path

import app.wordpace.inkwell.{DatabaseConfiguration, DefaultGeneratorConfiguration}

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
