package com.github.choppythelumberjack.trivialgen.schema

import scala.reflect.runtime.universe.Type

// TODO: Add the ability to navigate "up" the model to columns and tables.

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
  scalaType: Type,
  isNullable: Boolean,
  meta: JdbcColumnMeta,
)

object Column {
  type Name = String
}
