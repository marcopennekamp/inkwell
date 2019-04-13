package com.github.choppythelumberjack.trivialgen.integration

import java.nio.file.Path

import com.github.choppythelumberjack.trivialgen.{DatabaseConfiguration, DefaultGeneratorConfiguration}

object ConfigLoader {
  def singleFileConfig(sqlScriptName: String, target: Path, pkg: String) = DefaultGeneratorConfiguration(
    DatabaseConfiguration(s"jdbc:h2:mem:sample;INIT=RUNSCRIPT FROM 'generator/src/test/resources/$sqlScriptName'", "sa", ""),
    "PUBLIC",
    target,
    pkg,
  )
}
