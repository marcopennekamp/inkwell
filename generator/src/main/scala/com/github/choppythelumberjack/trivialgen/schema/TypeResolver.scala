package com.github.choppythelumberjack.trivialgen.schema

import java.sql.Types._

import scala.reflect.{ClassTag, classTag}

trait TypeResolver {
  /**
    * Resolves a JDBC type to a Scala type if possible. Override [[DefaultTypeResolver]] to support your own custom
    * types (such as enum types).
    */
  def apply(columnMeta: JdbcColumnMeta): Option[ClassTag[_]]
}

/**
  * Resolves common native JDBC types as well as user-defined types given in the [[jdbcToScala]] map.
  *
  * @param jdbcToScala A map of user-defined (or exotic) JDBC type names pointing to their corresponding
  *                    Scala ClassTag's.
  */
class DefaultTypeResolver(jdbcToScala: Map[String, ClassTag[_]]) extends TypeResolver {

  override def apply(columnMeta: JdbcColumnMeta): Option[ClassTag[_]] = {
    implicit def toSome[T](tag: ClassTag[_]): Some[ClassTag[_]] = Some(tag)

    // see TABLE B-1 of JSR-000221 JBDCTM API Specification 4.1 Maintenance Release Mapping
    // to corresponding Scala types where applicable.
    columnMeta.dataType match {
      case CHAR | VARCHAR | LONGVARCHAR | NCHAR | NVARCHAR | LONGNVARCHAR | CLOB => classTag[String]
      case NUMERIC | DECIMAL => classTag[BigDecimal]
      case BIT | BOOLEAN => classTag[Boolean]
      case TINYINT => classTag[Byte]
      case SMALLINT | INTEGER => classTag[Int]
      case BIGINT => classTag[Long]
      case REAL => classTag[Float]
      case FLOAT | DOUBLE => classTag[Double]
      case DATE => classTag[java.time.LocalDate]
      case TIME => classTag[java.time.LocalDateTime]
      case TIMESTAMP => classTag[java.time.LocalDateTime]
      case _ => jdbcToScala.get(columnMeta.typeName)
    }
  }

}
