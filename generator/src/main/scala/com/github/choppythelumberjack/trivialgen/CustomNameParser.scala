package com.github.choppythelumberjack.trivialgen

import com.github.choppythelumberjack.trivialgen.model.{ColumnMeta, TableMeta}
import com.github.choppythelumberjack.trivialgen.util.StringUtil._

sealed trait CustomNameParser {
  def generateQuerySchemas:Boolean = true
  def parseColumn(cm:ColumnMeta):String
  def parseTable(tm:TableMeta):String
}
sealed trait NameParser extends CustomNameParser { override def generateQuerySchemas:Boolean = false }

trait LiteralNames extends NameParser {
  def parseColumn(cm:ColumnMeta):String = cm.columnName
  def parseTable(tm:TableMeta):String = tm.tableName.capitalize
}
trait SnakeCaseNames extends NameParser {
  def parseColumn(cm:ColumnMeta):String = cm.columnName.snakeToLowerCamel
  def parseTable(tm:TableMeta):String = tm.tableName.snakeToUpperCamel
}

object LiteralNames extends LiteralNames
object SnakeCaseNames extends SnakeCaseNames

case class SnakeCaseCustomTable(
  tableParser: TableMeta => String
) extends CustomNameParser {
  def parseColumn(cm:ColumnMeta):String = cm.columnName.snakeToLowerCamel
  def parseTable(tm:TableMeta):String = tableParser(tm)
}

case class CustomStrategy(
  columnParser: ColumnMeta => String = cm=>cm.columnName.snakeToLowerCamel,
  tableParser: TableMeta => String = tm=>tm.tableName.snakeToUpperCamel
) extends CustomNameParser {
  def parseColumn(cm:ColumnMeta):String = columnParser(cm)
  def parseTable(tm:TableMeta):String = tableParser(tm)
}

object CustomNameParser {
  implicit class EntityNamingStrategyExtensions(strategy: CustomNameParser) {
    def nameTable(tableSchema: TableMeta) = CustomNameParser.nameTable(strategy, tableSchema)
    def nameColumn(columnSchema: ColumnMeta) = CustomNameParser.nameColumn(strategy, columnSchema)
  }

  def nameTable(strategy: CustomNameParser, tableSchema: TableMeta) =
    strategy.parseTable(tableSchema)

  def nameColumn(strategy: CustomNameParser, columnSchema: ColumnMeta) =
    strategy.parseColumn(columnSchema)
}
