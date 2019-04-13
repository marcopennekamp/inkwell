package com.github.choppythelumberjack.trivialgen.generator

import scala.reflect.runtime.universe.Type

/**
  * Turns a Scala [[Type]] into a string with the option of modifying the string <b>globally</b>.
  */
trait RawTypeBuilder {
  def apply(scalaType: Type): String
}

/**
  * Simplifies imported type and "java.lang._" type names.
  */
class ImportSimplifyingRawTypeBuilder(imports: Set[Import]) extends RawTypeBuilder {
  val classes: Set[String] =
    imports.filter(_.isInstanceOf[Import.Entity]).map { case e: Import.Entity => e.classTag.toString }
  val packages: Set[String] =
    imports.filter(_.isInstanceOf[Import.Package]).map { case p: Import.Package => p.name } + "java.lang" + "scala"

  override def apply(t: Type): String = {
    val fullName = t.typeSymbol.fullName
    println(s"Resolving full type name: $fullName")
    val nameParts = fullName.split('.')
    val packageName = Some(nameParts).filter(_.nonEmpty).map(_.init.mkString(".")).getOrElse("")
    val simpleName = nameParts.last

    if (packages.contains(packageName) || classes.contains(fullName)) {
      simpleName
    } else {
      fullName
    }
  }
}
