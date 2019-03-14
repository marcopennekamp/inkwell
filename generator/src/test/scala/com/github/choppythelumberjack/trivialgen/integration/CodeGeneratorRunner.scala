package com.github.choppythelumberjack.trivialgen.integration

import com.github.choppythelumberjack.trivialgen._
import com.github.choppythelumberjack.trivialgen.ext.{AutoDiscoveringGen, ComposeableTraitsGen, MirrorContext, TrivialGen}
import com.github.choppythelumberjack.trivialgen.model.StereotypingService.Namespacer
import com.github.choppythelumberjack.trivialgen.util.StringUtil._

object CodeGeneratorRunner extends TestConfigs {

  def main(args:Array[String]):Unit = {
    val path = args(0)
    trivialGen(path)
    composeableGen(path)
    composeableAutoDiscoveringGen(path)
  }

  def trivialGen(basePath:String) = {

    def pack(num:Int) = s"com.github.choppythelumberjack.trivialgen.generated.simp${num}"
    def path(num:Int) = s"${basePath}/com/github/choppythelumberjack/trivialgen/generated/simp${num}"

    val snakeCaseGen = new TrivialGen(snakecaseConfig, pack(0)) {
      override def nameParser = SnakeCaseNames
    }
    snakeCaseGen.writeFiles(path(0))

    val literalGen = new TrivialGen(literalConfig, pack(1)) {
      override def nameParser = LiteralNames // Should be default
    }
    literalGen.writeFiles(path(1))
  }

  def composeableGen(basePath:String) = {

    def pack(num:Int) = s"com.github.choppythelumberjack.trivialgen.generated.comp${num}"
    def path(num:Int) = s"${basePath}/com/github/choppythelumberjack/trivialgen/generated/comp${num}"

    // generate Composeable Schema in test project - trivial
    {
      val gen = new ComposeableTraitsGen(snakecaseConfig, pack(0), false) {
        override def nameParser: NameParser = SnakeCaseNames
      }
      gen.writeFiles(path(0))
    }

    // generate Composeable Schema in test project - simple
    {
      val gen = new ComposeableTraitsGen(snakecaseConfig, pack(1)) {
        override def nameParser: NameParser =
          CustomNames(
            col => col.columnName.toLowerCase.replace("_name", "")
          )
      }
      gen.writeFiles(path(1))
    }

    // generate Composeable Schema in test project - stereotyped one schema
    {
      val gen = new ComposeableTraitsGen(
        twoSchemaConfig, pack(2),
        nestedTrait = true)
      {
        override def nameParser: NameParser = CustomNames()
        override def memberNamer: MemberNamer = ts => (ts.tableSchem.toLowerCase + ts.tableName.snakeToUpperCamel)
        override val namespacer: Namespacer = ts =>
          if (ts.tableSchem.toLowerCase == "alpha" || ts.tableSchem.toLowerCase == "bravo") "public"
          else ts.tableSchem.toLowerCase
      }

      gen.writeFiles(path(2))
    }

    // generate Composeable Schema in test project - stereotyped multiple schemas
    {
      val gen = new ComposeableTraitsGen(twoSchemaConfig, pack(3), false)
      {
        override def nameParser: NameParser = CustomNames()
        override def memberNamer: MemberNamer = ts => (ts.tableSchem.toLowerCase + ts.tableName.snakeToUpperCamel)
        override val namespacer: Namespacer =
          ts => if (ts.tableSchem.toLowerCase == "alpha" || ts.tableSchem.toLowerCase == "bravo") "common" else ts.tableSchem.toLowerCase
      }

      gen.writeFiles(path(3))
    }

    // generate Composeable Schema in test project - non-stereotyped
    {
      val gen = new ComposeableTraitsGen(twoSchemaConfig, pack(4), nestedTrait = true) {
        override def nameParser: NameParser = CustomNames()
      }

      gen.writeFiles(path(4))
    }
  }

  def composeableAutoDiscoveringGen(basePath:String) {

    def pack(num:Int) = s"com.github.choppythelumberjack.trivialgen.generated.comp${num}"
    def path(num:Int) = s"${basePath}/com/github/choppythelumberjack/trivialgen/generated/comp${num}"

    // using mirror context - simple
    {
      val gen = new AutoDiscoveringGen(snakecaseConfig, MirrorContext, pack(5), false) {
        override def nameParser: NameParser = CustomNames()
      }

      gen.writeFiles(path(5))
    }

    // using mirror context - stereotyped multiple schemas
    {
      val gen = new AutoDiscoveringGen(twoSchemaConfig, MirrorContext, pack(6), false)
      {
        override def memberNamer: MemberNamer = ts => (ts.tableSchem.toLowerCase + ts.tableName.snakeToUpperCamel)
        override def nameParser: NameParser = CustomNames()
        override val namespacer: Namespacer =
          ts => if (ts.tableSchem.toLowerCase == "alpha" || ts.tableSchem.toLowerCase == "bravo") "common" else ts.tableSchem.toLowerCase
      }

      gen.writeFiles(path(6))
    }

    // generate Composeable Schema in test project - non-stereotyped
    {
      val gen = new AutoDiscoveringGen(twoSchemaConfig, MirrorContext, pack(7), nestedTrait = true) {
        override def nameParser: NameParser = CustomNames()
      }

      gen.writeFiles(path(7))
    }
  }

  // TODO Test for stereotyping in multiple databases
}
