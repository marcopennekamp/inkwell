package app.wordpace.inkwell.schema

import app.wordpace.inkwell.generator.TypeReference

case class Schema(tables: Seq[Table]) {
  lazy val tableMap: Map[Table.Name, Table] = tables.map(t => t.name -> t).toMap
}

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
  dataType: TypeReference,
  isNullable: Boolean,
  meta: JdbcColumnMeta,
) {
  /**
    * The parent table of the column. The property is safely initialised by [[SchemaReader]].
    */
  var table: Table = _

  /**
    * Assuming this column is a foreign key, it points to a specific table's columns (which is usually the
    * primary key, but may be a unique key). If this column is not a foreign key, the sequence is simply empty.
    * The name 'references' mirrors the SQL keyword. The property is safely initialised by [[SchemaReader]].
    */
  var references: Seq[Column] = Seq.empty
}

object Column {
  type Name = String
}
