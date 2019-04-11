package com.github.choppythelumberjack.trivialgen.schema

import com.github.choppythelumberjack.trivialgen.ext.DatabaseTypes.{DatabaseType, MySql}

trait SchemaNameResolver {
  /**
    * @param catalog The [[JdbcTableMeta.tableCatalog]] or [[JdbcColumnMeta.tableCatalog]] entry.
    * @param schema The [[JdbcTableMeta.tableSchema]] or [[JdbcColumnMeta.tableSchema]] entry.
    * @return The schema name resolved either from the catalog or the schema meta entry.
    */
  def apply(catalog: String, schema: String): String
}

/**
  * The default schema name resolver, which resolves a MySQL schema name by the catalog entry and other schema names
  * by the schema entry.
  */
class DefaultSchemaNameResolver(databaseType: DatabaseType) extends SchemaNameResolver {
  override def apply(catalog: String, schema: String): String = databaseType match {
    case MySql => catalog
    case _ => schema
  }
}
