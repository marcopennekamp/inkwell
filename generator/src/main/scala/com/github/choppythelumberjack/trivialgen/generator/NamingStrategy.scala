package com.github.choppythelumberjack.trivialgen.generator

trait NamingStrategy {
  def table(sqlName: String): String = name(sqlName)
  def column(sqlName: String): String = name(sqlName)
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
  * This strategy will turn your <b>Scala names</b> into snake case! In 95% of cases, you probably want
  * to use [[CamelCase]] instead. (That is, NamingStrategies are "reversed" in terms of intended usage.)
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
  * Table example (sql name => scala name): some_ident => SomeIdent
  * Column example (sql name => scala name): some_ident => someIdent
  */
trait CamelCase extends NamingStrategy {
  override def name(sqlName: String): String = io.getquill.CamelCase.default(sqlName)
  override def table(sqlName: String): String = name(sqlName).capitalize
}

object CamelCase extends CamelCase
