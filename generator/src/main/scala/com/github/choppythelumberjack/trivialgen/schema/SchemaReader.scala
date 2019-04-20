package com.github.choppythelumberjack.trivialgen.schema

import java.sql.{Connection, DatabaseMetaData, DriverManager}

import com.github.choppythelumberjack.trivialgen.GeneratorConfiguration
import com.github.choppythelumberjack.trivialgen.schema.SchemaReader.UnknownJdbcTypeException

import scala.util.Try

trait SchemaReader {
  /**
    * Reads the source schema from the database.
    */
  def read(): Try[Schema]
}

object SchemaReader {
  case class UnknownJdbcTypeException(columnMeta: JdbcColumnMeta, tableName: String)
    extends RuntimeException(s"Could not resolve Scala type for JDBC type ${columnMeta.dataType}/${columnMeta.typeName}" +
      s" of column ${columnMeta.columnName} in table $tableName")
}

class DefaultSchemaReader(config: GeneratorConfiguration) extends SchemaReader {

  override def read(): Try[Schema] = {
    var db: Option[Connection] = None
    val computation = Try {
      db = Some(DriverManager.getConnection(config.db.url, config.db.username, config.db.password))
      getSchema(db.get)
    }
    db.foreach(_.close())
    computation
  }

  protected def getSchema(db: Connection): Schema = {
    val schema = Schema(getTables(db))
    schema.tables.foreach(_.schema = schema) // Setup "upwards" references.
    schema
  }

  import com.github.choppythelumberjack.trivialgen.util.ResultSetExtensions

  // Note that in the following code, toVector unravels the iterator, so that we don't access the DB (through maps)
  // after closing it. The more general toSeq does NOT transform the iterator to a list but instead keeps the iterator.

  // TODO: Handle primary keys (for IDs).
  // TODO: Handle foreign keys.

  protected def getTables(db: Connection): Seq[Table] = {
    val rs = db.getMetaData.getTables(null, config.sourceSchema, "%", Array("TABLE")) // The pattern % matches any name.
    val metas = rs.toIterator.map(rs => JdbcTableMeta.fromResultSet(rs))
      .filterNot(m => config.ignoredTables.contains(m.tableName)) // Exclude ignored tables.
      .toVector

    metas.map { meta =>
      val columns = getColumns(db, meta.tableName)
      val table = Table(meta.tableName, columns, meta)
      columns.foreach(_.table = table) // Setup "upwards" references.
      table
    }
  }

  protected def getColumns(db: Connection, tableName: Table.Name): Seq[Column] = {
    val rs = db.getMetaData.getColumns(null, config.sourceSchema, tableName, "%") // The pattern % matches any name.
    val metas = rs.toIterator.map(rs => JdbcColumnMeta.fromResultSet(rs))

    metas.map { meta =>
      val dataType = config.typeResolver(meta).getOrElse(throw UnknownJdbcTypeException(meta, tableName))
      val isNullable = meta.nullable == DatabaseMetaData.columnNullable
      Column(meta.columnName, dataType, isNullable, meta)
    }.toVector
  }

}
