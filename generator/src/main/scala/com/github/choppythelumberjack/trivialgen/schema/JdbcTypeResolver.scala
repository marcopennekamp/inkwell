package com.github.choppythelumberjack.trivialgen.schema

import java.sql.Types._

import scala.reflect.{ClassTag, classTag}

trait JdbcTypeResolver {
  /**
    * Resolves a JDBC type to a Scala type if possible. Override [[DefaultJdbcTypeResolver]] to support your own custom
    * types (such as enum types).
    */
  def apply(columnMeta: JdbcColumnMeta): Option[ClassTag[_]]
}

class DefaultJdbcTypeResolver extends JdbcTypeResolver {

  override def apply(columnMeta: JdbcColumnMeta): Option[ClassTag[_]] = {
    implicit def toSome[T](tag: ClassTag[_]): Some[ClassTag[_]] = Some(tag)

    // see TABLE B-1 of JSR-000221 JBDCTM API Specification 4.1 Maintenance Release Mapping
    // to corresponding Scala types where applicable.
    columnMeta.dataType match {
      case CHAR | VARCHAR | LONGVARCHAR | NCHAR | NVARCHAR | LONGNVARCHAR => classTag[String]
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
      case _ => None
    }
  }

}
