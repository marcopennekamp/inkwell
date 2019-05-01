package app.wordpace.inkwell.schema

import java.sql.ResultSet

case class JdbcTableMeta(
  tableCatalog: String,
  tableSchema: String,
  tableName: String,
  tableType: String,
)

object JdbcTableMeta {
  def fromResultSet(rs:ResultSet) = JdbcTableMeta(
    tableCatalog = rs.getString("TABLE_CAT"),
    tableSchema = rs.getString("TABLE_SCHEM"),
    tableName = rs.getString("TABLE_NAME"),
    tableType = rs.getString("TABLE_TYPE"),
  )
}

case class JdbcColumnMeta(
  tableCatalog: String,
  tableSchema: String,
  tableName: String,
  columnName: String,
  dataType: Int,
  typeName: String,
  columnSize: Int,
  decimalDigits: Int,
  numPrecRadix: Int,
  nullable: Int,
  columnDefault: String,
  charOctetLength: Int,
  ordinalPosition: Int,
  sourceDataType: Int,
  isAutoincrement: String,
)

object JdbcColumnMeta {
  def fromResultSet(rs:ResultSet) =
    JdbcColumnMeta(
      tableCatalog = rs.getString("TABLE_CAT"),
      tableSchema = rs.getString("TABLE_SCHEM"),
      tableName = rs.getString("TABLE_NAME"),
      columnName = rs.getString("COLUMN_NAME"),
      dataType = rs.getInt("DATA_TYPE"),
      typeName = rs.getString("TYPE_NAME"),
      columnSize = rs.getInt("COLUMN_SIZE"),
      decimalDigits = rs.getInt("DECIMAL_DIGITS"),
      numPrecRadix = rs.getInt("NUM_PREC_RADIX"),
      nullable = rs.getInt("NULLABLE"),
      columnDefault = rs.getString("COLUMN_DEF"),
      charOctetLength = rs.getInt("CHAR_OCTET_LENGTH"),
      ordinalPosition = rs.getInt("ORDINAL_POSITION"),
      sourceDataType = rs.getInt("SOURCE_DATA_TYPE"),
      isAutoincrement = rs.getString("IS_AUTOINCREMENT"),
    )
}

case class ColumnIdentifier(tableName: Table.Name, columnName: Column.Name)

case class JdbcForeignKeyMeta(from: ColumnIdentifier, to: ColumnIdentifier)
