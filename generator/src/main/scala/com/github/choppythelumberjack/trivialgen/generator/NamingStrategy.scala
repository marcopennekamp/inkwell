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
  * This strategy will make your <b>Scala names</b> to adhere to snake_case! In 95% of cases, you probably want
  * to use [[CamelCase]] instead. (That is, NamingStrategies are "reversed" in terms of intended usage compared
  * to Quill.)
  *
  * Example (sql name => scala name): someIdent => some_ident
  */
trait SnakeCase extends NamingStrategy {
  override def name(sqlName: String): String = io.getquill.SnakeCase.default(sqlName)
}

object SnakeCase extends SnakeCase

/**
  * Turns table names to UpperCamelCase and column names to lowerCamelCase.
  *
  * Model example (sql name => scala name): some_ident => SomeIdent
  * Property example (sql name => scala name): some_ident => someIdent
  */
trait CamelCase extends NamingStrategy {
  override def name(sqlName: String): String = io.getquill.CamelCase.default(sqlName)
  override def model(sqlName: String): String = name(sqlName).capitalize
}

object CamelCase extends CamelCase
