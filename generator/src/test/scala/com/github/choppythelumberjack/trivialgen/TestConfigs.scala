package com.github.choppythelumberjack.trivialgen

import com.github.choppythelumberjack.trivialgen.generator.CodeGeneratorConfig

trait TestConfigs {
  def makeConfig(script: String) = CodeGeneratorConfig(
    "sa", "sa", s"jdbc:h2:mem:sample;INIT=RUNSCRIPT FROM 'generator/src/test/resources/${script}'"
  )

  def twoSchemaConfig = makeConfig("schema_snakecase_twoschema_differentcolumns_differenttypes.sql")
  def snakecaseConfig = makeConfig("schema_snakecase.sql")
  def literalConfig = makeConfig("schema_casesensitive.sql")
}
