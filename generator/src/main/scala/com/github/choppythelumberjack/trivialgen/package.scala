package com.github.choppythelumberjack

import java.sql.Connection

import com.github.choppythelumberjack.trivialgen.ext.DatabaseTypes.{DatabaseType, MySql, Postgres}
import com.github.choppythelumberjack.trivialgen.model._
import com.github.choppythelumberjack.trivialgen.schema.{JdbcColumnMeta, JdbcTableMeta, Schema, TableSchema}

import scala.reflect.ClassTag

package object trivialgen {

  case class JdbcTypeInfo(jdbcType:Int, typeName:String = "")
  object JdbcTypeInfo {
    def apply(cs: JdbcColumnMeta):JdbcTypeInfo = JdbcTypeInfo(cs.dataType, cs.typeName)
  }

  type GeneratorEngine = (Seq[TableStereotype] => Seq[String])
  type MemberNamer = JdbcTableMeta => String
}
