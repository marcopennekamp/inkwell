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
    override val context = classOf[H2JdbcContext[_]]
    override val dialect = classOf[H2Dialect]
  }

  case object MySql extends DatabaseType {
    override val databaseName = "MySQL"
    override val context = classOf[MysqlJdbcContext[_]]
    override val dialect = classOf[MySQLDialect]
  }

  case object SqlServer extends DatabaseType {
    override val databaseName = "Microsoft SQL Server"
    override val context = classOf[SqlServerJdbcContext[_]]
    override val dialect = classOf[SQLServerDialect]
  }

  case object Postgres extends DatabaseType {
    override val databaseName = "PostgreSQL"
    override val context = classOf[PostgresJdbcContext[_]]
    override val dialect = classOf[PostgresDialect]
  }

  case object Sqlite extends DatabaseType {
    override val databaseName = "SQLite"
    override val context = classOf[SqliteJdbcContext[_]]
    override val dialect = classOf[SqliteDialect]
  }
}
