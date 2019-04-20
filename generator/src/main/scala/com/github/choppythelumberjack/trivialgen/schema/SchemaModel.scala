package com.github.choppythelumberjack.trivialgen.schema

import scala.reflect.runtime.universe.Type

// TODO: Add the ability to navigate "up" the model to columns and tables.
//       This is required for getting a table or schema object in a PropertyEmitter, for example.

case class Schema(
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
