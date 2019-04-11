package com.github.choppythelumberjack.trivialgen

import com.github.choppythelumberjack.trivialgen.model.DefaultStereotypingService

package object generator {
  // TODO: Move this into CodeGeneratorComponents and treat the latter as a universal config trait which can be overridden.
  case class CodeGeneratorConfig(
    targetFolder: String,
    targetSchemaName: String,
    username: String,
    password: String,
    url: String,
  )

  // TODO: Rip this out.
  trait GeneratorBase extends Generator { this: CodeGeneratorComponents =>
  }

  class StandardGenerator(val configs: Seq[CodeGeneratorConfig], val packagePrefix:String)
    extends GeneratorBase with StandardCodeGeneratorComponents {

    def this(config:CodeGeneratorConfig) = this(Seq(config), "")
  }
}
