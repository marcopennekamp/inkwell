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

    // Setup foreign keys now so we can build proper references to Column instances.
    val foreignKeyMetas = getForeignKeyMetas(db)
    schema.tables.foreach { table =>
      // Grouping the table's foreign keys by from.columnName essentially gives us one sequence element
      // per individual foreign key. For example, if a foreign key references two columns of the other
      // table, foreignKeyMetas would contain two entries while they would be combined in foreignKeys.
      val foreignKeys = foreignKeyMetas.filter(_.from.tableName == table.name).groupBy(_.from.columnName)
      foreignKeys.foreach { case (columnName, fkMeta) =>
        table.columns.find(_.name == columnName) match {
          case None => throw new RuntimeException(s"Inconsistent database schema: Unknown column name $columnName")
          case Some(column) =>
            // Here, we can assume two things:
            //  (1) Due to the way how groupBy works, fkMeta must contain at least one element. Hence we can
            //      safely call head.
            //  (2) Since we had filtered by from.tableName earlier, we can safely assume the table name of
            //      any fkMeta entry is the same.
            val targetTableName = fkMeta.head.to.tableName
            schema.tableMap.get(targetTableName) match {
              case None => throw new RuntimeException(s"Inconsistent database schema: Unknown table name $targetTableName")
              case Some(targetTable) =>
                // Get all columns referenced by the foreign key. We don't care what these columns are exactly,
                // but just assume that the database has ensured the consistency of the schema (such that, for
                // example, only primary keys or unique keys can be targets of a foreign key).
                val targetColumnNames = fkMeta.map(_.to.columnName)
                val targetColumns = targetTable.columns.filter(c => targetColumnNames.contains(c.name))
                column.references = targetColumns
            }
        }
      }
    }

    // Setup "upwards" references.
    schema.tables.foreach(_.schema = schema)
    schema
  }

  import com.github.choppythelumberjack.trivialgen.util.ResultSetExtensions

  // Note that in the following code, toVector unravels the iterator, so that we don't access the DB (through maps)
  // after closing it. The more general toSeq does NOT transform the iterator to a list but instead keeps the iterator.

  protected def getTables(db: Connection): Seq[Table] = {
    val rs = db.getMetaData.getTables(null, config.sourceSchema, "%", Array("TABLE")) // The pattern % matches any name.
    val metas = rs.toIterator.map(rs => JdbcTableMeta.fromResultSet(rs))
      .filterNot(m => config.ignoredTables.contains(m.tableName)) // Exclude ignored tables.
      .toVector

    metas.map { meta =>
      val columns = getColumns(db, meta.tableName)
      val primaryKeyNames = getPrimaryKeyNames(db, meta.tableName)
      val primaryKey = columns.filter(c => primaryKeyNames.contains(c.name))
      assert(primaryKeyNames.length == primaryKey.length)
      val table = Table(meta.tableName, columns, primaryKey, meta)

      // Setup "upwards" references.
      columns.foreach(_.table = table)
      table
    }
  }

  protected def getPrimaryKeyNames(db: Connection, tableName: String): Seq[String] = {
    val rs = db.getMetaData.getPrimaryKeys(null, null, tableName)
    rs.toIterator.map { row =>
      row.getString("COLUMN_NAME")
    }.toVector
  }

  protected def getColumns(db: Connection, tableName: Table.Name): Seq[Column] = {
    val rs = db.getMetaData.getColumns(null, config.sourceSchema, tableName, "%") // The pattern % matches any name.
    val metas = rs.toIterator.map(rs => JdbcColumnMeta.fromResultSet(rs)).toVector

    metas.map { meta =>
      val dataType = config.typeResolver(meta).getOrElse(throw UnknownJdbcTypeException(meta, tableName))
      val isNullable = meta.nullable == DatabaseMetaData.columnNullable
      Column(meta.columnName, dataType, isNullable, meta)
    }
  }

  /**
    * Note that foreign keys are read in "backwards". JDBC returns the foreign keys pointing to a primary key
    * of a specific table, so we have to get them all at once and then resolve at the end.
    */
  protected def getForeignKeyMetas(db: Connection): Seq[JdbcForeignKeyMeta] = {
    val rs = db.getMetaData.getExportedKeys(null, config.sourceSchema, null)
    rs.toIterator.map { row =>
      JdbcForeignKeyMeta(
        from = ColumnIdentifier(
          tableName = row.getString("FKTABLE_NAME"),
          columnName = row.getString("FKCOLUMN_NAME")
        ),
        to = ColumnIdentifier(
          tableName = row.getString("PKTABLE_NAME"),
          columnName = row.getString("PKCOLUMN_NAME")
        )
      )
    }.toVector
  }

}
