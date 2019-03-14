package com.github.choppythelumberjack.trivialgen

import com.github.choppythelumberjack.trivialgen.model.{ColumnMeta, TableMeta}
import com.github.choppythelumberjack.trivialgen.util.StringUtil._

sealed trait NameParser {
  def generateQuerySchemas:Boolean
  def parseColumn(cm:ColumnMeta):String
  def parseTable(tm:TableMeta):String
  def idiom: Class[_ <: io.getquill.NamingStrategy]
}

trait LiteralNames extends NameParser {
  def generateQuerySchemas = false
  def parseColumn(cm:ColumnMeta):String = cm.columnName
  def parseTable(tm:TableMeta):String = tm.tableName.capitalize
  def idiom = classOf[io.getquill.Literal]
}
trait SnakeCaseNames extends NameParser {
  def generateQuerySchemas = false
  def parseColumn(cm:ColumnMeta):String = cm.columnName.snakeToLowerCamel
  def parseTable(tm:TableMeta):String = tm.tableName.snakeToUpperCamel
  def idiom = classOf[io.getquill.SnakeCase]
}

object LiteralNames extends LiteralNames
object SnakeCaseNames extends SnakeCaseNames

case class CustomNames(
  columnParser: ColumnMeta => String = cm=>cm.columnName.snakeToLowerCamel,
  tableParser: TableMeta => String = tm=>tm.tableName.snakeToUpperCamel
) extends NameParser {
  def generateQuerySchemas = true
  def parseColumn(cm:ColumnMeta):String = columnParser(cm)
  def parseTable(tm:TableMeta):String = tableParser(tm)
  def idiom = classOf[io.getquill.Literal]
}

case class SnakeCaseCustomTable(
  tableParser: TableMeta => String
) extends NameParser {
  def generateQuerySchemas = true
  def parseColumn(cm:ColumnMeta):String = cm.columnName.snakeToLowerCamel
  def parseTable(tm:TableMeta):String = tableParser(tm)
  def idiom = classOf[io.getquill.SnakeCase]
}
