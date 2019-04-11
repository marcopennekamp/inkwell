package com.github.choppythelumberjack.trivialgen.schema

import io.getquill._
import io.getquill.context.jdbc.JdbcContext
import io.getquill.context.sql.idiom.SqlIdiom

sealed trait DatabaseType {
  def databaseName: String
  def context: Class[_ <: JdbcContext[_, _]]
  def dialect: Class[_ <: SqlIdiom]
}

object DatabaseType {
  private def all: Seq[DatabaseType] = Seq(H2, MySql, SqlServer, Postgres, Sqlite)

  def fromProductName(productName: String): DatabaseType = {
    val res = all.find(_.databaseName == productName)
    res.getOrElse(
      throw new IllegalArgumentException(s"Database type $productName not supported." +
        s"Possible Values are: ${all.map(_.databaseName)}")
    )
  }

  case object H2 extends DatabaseType {
    override val databaseName = "H2"
    override val context: Class[H2JdbcContext[_]] = classOf[H2JdbcContext[_]]
    override val dialect: Class[H2Dialect] = classOf[H2Dialect]
  }

  case object MySql extends DatabaseType {
    override val databaseName = "MySQL"
    override val context: Class[MysqlJdbcContext[_]] = classOf[MysqlJdbcContext[_]]
    override val dialect: Class[MySQLDialect] = classOf[MySQLDialect]
  }

  case object SqlServer extends DatabaseType {
    override val databaseName = "Microsoft SQL Server"
    override val context: Class[SqlServerJdbcContext[_]] = classOf[SqlServerJdbcContext[_]]
    override val dialect: Class[SQLServerDialect] = classOf[SQLServerDialect]
  }

  case object Postgres extends DatabaseType {
    override val databaseName = "PostgreSQL"
    override val context: Class[PostgresJdbcContext[_]] = classOf[PostgresJdbcContext[_]]
    override val dialect: Class[PostgresDialect] = classOf[PostgresDialect]
  }

  case object Sqlite extends DatabaseType {
    override val databaseName = "SQLite"
    override val context: Class[SqliteJdbcContext[_]] = classOf[SqliteJdbcContext[_]]
    override val dialect: Class[SqliteDialect] = classOf[SqliteDialect]
  }
}
