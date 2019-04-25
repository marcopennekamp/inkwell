package app.wordpace.inkwell.schema

import java.sql.Types._

import app.wordpace.inkwell.generator.{ScalaTypeReference, TypeReference}

import scala.reflect.runtime.universe.{Type, typeOf}

trait TypeResolver {
  /**
    * Resolves a JDBC type to a [[TypeReference]] if possible.
    */
  def apply(columnMeta: JdbcColumnMeta): Option[TypeReference]
}

/**
  * Resolves common native JDBC types as well as user-defined types given in the [[jdbcToScala]] map.
  *
  * @param jdbcToScala A map of user-defined (or exotic) JDBC type names pointing to their corresponding
  *                    type references.
  */
class DefaultTypeResolver(jdbcToScala: Map[String, TypeReference]) extends TypeResolver {

  override def apply(columnMeta: JdbcColumnMeta): Option[TypeReference] = {
    implicit def toSomeScalaTypeReference[T](t: Type): Option[TypeReference] = Some(ScalaTypeReference(t))

    // Check custom type names first, since custom types may appear to have a standard data type
    // regardless (such as Postgres enums being treated as strings).
    jdbcToScala.get(columnMeta.typeName).orElse {
      columnMeta.dataType match {
        case CHAR | VARCHAR | LONGVARCHAR | NCHAR | NVARCHAR | LONGNVARCHAR | CLOB => typeOf[String]
        case NUMERIC | DECIMAL => typeOf[BigDecimal]
        case BIT | BOOLEAN => typeOf[Boolean]
        case TINYINT => typeOf[Byte]
        case SMALLINT => typeOf[Short]
        case INTEGER => typeOf[Int]
        case BIGINT => typeOf[Long]
        case REAL => typeOf[Float]
        case FLOAT | DOUBLE => typeOf[Double]
        case BINARY | VARBINARY | LONGVARBINARY => typeOf[Array[Byte]]
        case DATE => typeOf[java.time.LocalDate]
        case TIME => typeOf[java.time.LocalDateTime]
        case TIMESTAMP => typeOf[java.time.LocalDateTime]
        case _ => None
      }
    }
  }

}
