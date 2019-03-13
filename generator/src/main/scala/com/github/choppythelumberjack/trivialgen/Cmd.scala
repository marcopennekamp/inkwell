package com.github.choppythelumberjack.trivialgen

import java.io.File
import scopt.OParser

object Cmd {
  def main(args:Array[String]):Unit = {
    val builder = OParser.builder[CmdConfig]
    val parser = {
      import builder._
      OParser.sequence(
        programName("Quill Code Generator"),
        opt[String]('p', "names")
          .action((x, c) => {
            val np: CmdNameParser =
              x match {
                case "literal" => LiteralCmdNameParser
                case "snake" => SnakeCaseCmdNameParser
              }
            c.copy(nameParser = np)
          })
          .text("Simple parser for database names. Either 'literal', or 'snake_case'")
          // TODO Finish

      )
    }
  }
}

trait CmdNameParser
case object LiteralCmdNameParser extends CmdNameParser
case object SnakeCaseCmdNameParser extends CmdNameParser
case class RegexCmdNameParser(
  columnReplaceRegex: Option[(String, String)],
  tableReplaceRegex: Option[(String, String)]
)

case class CmdConfig(
  nameParser: CmdNameParser,
  pack:String,
  url:String,
  username:String,
  password:String,
  outputPath: String
)