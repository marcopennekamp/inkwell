package com.github.choppythelumberjack.trivialgen.generator

import scala.reflect.ClassTag

/**
  * Turns a [[ClassTag]] into a string with the option of modifying the string <b>globally</b>.
  */
trait RawTypeBuilder {
  def apply(dataType: ClassTag[_]): String
}

/**
  * Simplifies imported type and "java.lang._" type names.
  */
class ImportSimplifyingRawTypeBuilder(imports: Set[Import]) extends RawTypeBuilder {
  val classes: Set[ClassTag[_]] =
    imports.filter(_.isInstanceOf[Import.Entity]).map { case e: Import.Entity => e.classTag }
  val packages: Set[String] =
    imports.filter(_.isInstanceOf[Import.Package]).map { case p: Import.Package => p.name } + "java.lang"

  override def apply(dataType: ClassTag[_]): String = {
    val cl = dataType.runtimeClass
    val packageName = cl.getPackageName

    if (packages.contains(packageName) || classes.contains(dataType)) {
      cl.getSimpleName
    } else {
      dataType.toString
    }
  }
}
