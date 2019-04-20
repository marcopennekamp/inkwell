package com.github.choppythelumberjack.trivialgen.schema

import scala.reflect.runtime.universe.Type

case class Schema(
  tables: Seq[Table],
)

case class Table(
  name: Table.Name,
  columns: Seq[Column],
  primaryKey: Seq[Column],
  meta: JdbcTableMeta,
) {
  /**
    * The parent schema of the table. The property is safely initialised by [[SchemaReader]].
    */
  var schema: Schema = _
}

object Table {
  type Name = String
}

case class Column(
  name: Column.Name,
  scalaType: Type,
  isNullable: Boolean,
  meta: JdbcColumnMeta,
) {
  /**
    * The parent table of the column. The property is safely initialised by [[SchemaReader]].
    */
  var table: Table = _
}

object Column {
  type Name = String
}
