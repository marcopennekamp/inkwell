package com.github.choppythelumberjack.trivialgen

import java.sql.Connection

import com.github.choppythelumberjack.trivialgen.generator.{CodeGeneratorConfig, StandardGenerator}

/**
  * The purpose of the trivial code generator is to generate simple case classes representing tables
  * in a database. Create one or multiple <code>CodeGeneratorConfig</code> objects
  * and call the <code>.writeFiles</code> or <code>.writeStrings</code> methods
  * on the code generator and the rest happens automatically.
  */
// TODO: Rename to DefaultGenerator
class TrivialGen(
  override val config: CodeGeneratorConfig,
  override val db: Connection,
) extends StandardGenerator(config, packagePrefix) {
  override def nameParser: NameParser = LiteralNames
}
