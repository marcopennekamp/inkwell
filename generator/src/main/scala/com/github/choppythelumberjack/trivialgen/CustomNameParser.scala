package com.github.choppythelumberjack.trivialgen

import com.github.choppythelumberjack.trivialgen.model.{ColumnMeta, TableMeta}
import com.github.choppythelumberjack.trivialgen.util.StringUtil._

sealed trait CustomNameParser { def generateQuerySchemas:Boolean = true }
sealed trait NameParser extends CustomNameParser { override def generateQuerySchemas:Boolean = false }

case object LiteralNames extends NameParser
case object SnakeCaseNames extends NameParser

case class SnakeCaseCustomTable(
  tableParser: TableMeta => String
) extends CustomNameParser
case class CustomStrategy(
  columnParser: ColumnMeta => String = cm=>cm.columnName.snakeToLowerCamel,
  tableParser: TableMeta => String = tm=>tm.tableName.snakeToUpperCamel
) extends CustomNameParser

object CustomNameParser {
  implicit class EntityNamingStrategyExtensions(strategy: CustomNameParser) {
    def nameTable(tableSchema: TableMeta) = CustomNameParser.nameTable(strategy, tableSchema)
    def nameColumn(columnSchema: ColumnMeta) = CustomNameParser.nameColumn(strategy, columnSchema)
  }

  def nameTable(strategy: CustomNameParser, tableSchema: TableMeta) = {
    strategy match {
      case LiteralNames => tableSchema.tableName.capitalize
      case SnakeCaseNames => tableSchema.tableName.snakeToUpperCamel
      case SnakeCaseCustomTable(tableParser) => tableParser(tableSchema)
      case CustomStrategy(_, tableParser) => tableParser(tableSchema)
    }
  }
  def nameColumn(strategy: CustomNameParser, columnSchema: ColumnMeta) = {
    strategy match {
      case LiteralNames => columnSchema.columnName
      case SnakeCaseNames => columnSchema.columnName.snakeToLowerCamel
      case SnakeCaseCustomTable(_) => columnSchema.columnName.snakeToLowerCamel
      case c:CustomStrategy => c.columnParser(columnSchema)
    }
  }
}
