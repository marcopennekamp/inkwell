package app.wordpace.inkwell.schema

import java.sql.Types._

import scala.reflect.runtime.universe.{Type, typeOf}

trait TypeResolver {
  /**
    * Resolves a JDBC type to a Scala type if possible. Override [[DefaultTypeResolver]] to support your own custom
    * types (such as enum types).
    */
  def apply(columnMeta: JdbcColumnMeta): Option[Type]
}

/**
  * Resolves common native JDBC types as well as user-defined types given in the [[jdbcToScala]] map.
  *
  * @param jdbcToScala A map of user-defined (or exotic) JDBC type names pointing to their corresponding
  *                    Scala types.
  */
class DefaultTypeResolver(jdbcToScala: Map[String, Type]) extends TypeResolver {

  override def apply(columnMeta: JdbcColumnMeta): Option[Type] = {
    implicit def toSome[T](t: Type): Option[Type] = Some(t)

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
      case _ => jdbcToScala.get(columnMeta.typeName)
    }
  }

}
