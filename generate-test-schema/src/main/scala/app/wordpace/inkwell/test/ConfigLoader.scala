package app.wordpace.inkwell.test

import java.nio.file.Path

import app.wordpace.inkwell.{DatabaseConfiguration, DefaultGeneratorConfiguration}

object ConfigLoader {
  val defaultSchemaName = "PUBLIC"

  def databaseConfiguration(sqlScriptName: String, scriptRoot: String = ""): DatabaseConfiguration = {
    val url = s"jdbc:h2:mem:sample;INIT=RUNSCRIPT FROM '${scriptRoot}generate-test-schema/src/main/resources/$sqlScriptName'"
    DatabaseConfiguration(url, "sa", "")
  }

  def singleFileConfig(sqlScriptName: String, target: Path, basePackage: String) = DefaultGeneratorConfiguration(
    databaseConfiguration(sqlScriptName),
    defaultSchemaName,
    target,
    basePackage,
  )
}
