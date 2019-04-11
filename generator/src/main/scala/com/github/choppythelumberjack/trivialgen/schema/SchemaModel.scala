package com.github.choppythelumberjack.trivialgen.schema

import scala.reflect.ClassTag

case class Schema(
  databaseType: DatabaseType,
  tables: Seq[Table],
)

case class Table(
  name: Table.Name,
  columns: Seq[Column],
  meta: JdbcTableMeta,
)

object Table {
  type Name = String
}

case class Column(
  name: Column.Name,
  dataType: ClassTag[_],
  isNullable: Boolean,
  meta: JdbcColumnMeta,
)

object Column {
  type Name = String
}