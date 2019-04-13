package com.github.choppythelumberjack.trivialgen.generator

trait NamingStrategy {
  /**
    * The naming strategy for turning table names into model (case class) names.
    * See [[ModelEmitter]].
    */
  def model(sqlName: String): String = name(sqlName)

  /**
    * The naming strategy for turning column names into property names.
    * See [[PropertyEmitter]].
    */
  def property(sqlName: String): String = name(sqlName)

  /**
    * The common naming strategy.
    */
  def name(sqlName: String): String
}

/**
  * Example (sql name => scala name): some_ident => some_ident
  */
trait Literal extends NamingStrategy {
  override def name(s: String): String = s
}

object Literal extends Literal

/**
  * Example (sql name => scala name): some_ident => SOME_IDENT
  */
trait UpperCase extends NamingStrategy {
  override def name(sqlName: String): String = sqlName.toUpperCase
}

object UpperCase extends UpperCase

/**
  * Example (sql name => scala name): SOME_IDENT => some_ident
  */
trait LowerCase extends NamingStrategy {
  override def name(sqlName: String): String = sqlName.toLowerCase
}

object LowerCase extends LowerCase

/**
  * This strategy will make your <b>Scala names</b> adhere to snake_case! In 95% of cases, you probably want
  * to use [[SnakeCaseToCamelCase]] instead. (That is, NamingStrategies are "reversed" in terms of intended
  * usage compared to Quill.)
  *
  * Example (sql name => scala name): someIdent => some_ident
  */
trait CamelCaseToSnakeCase extends NamingStrategy {
  override def name(sqlName: String): String = io.getquill.SnakeCase.default(sqlName)
}

object CamelCaseToSnakeCase extends CamelCaseToSnakeCase

/**
  * Turns table names to UpperCamelCase and column names to lowerCamelCase.
  *
  * Model example (sql name => scala name): some_ident => SomeIdent
  * Property example (sql name => scala name): some_ident => someIdent
  */
trait SnakeCaseToCamelCase extends NamingStrategy {
  // The sqlName is lowercased because some databases seem to return case-insensitive names in upper case
  // and snake_case names are supposed to be lower case only.
  override def name(sqlName: String): String = io.getquill.CamelCase.default(sqlName.toLowerCase)
  override def model(sqlName: String): String = name(sqlName).capitalize
}

object SnakeCaseToCamelCase extends SnakeCaseToCamelCase
