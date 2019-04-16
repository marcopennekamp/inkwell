package com.github.choppythelumberjack.trivialgen.generator

import scala.reflect.runtime.universe._
import TypeUtil.TypeExtensions
import com.github.choppythelumberjack.trivialgen.generator.TypeUtil.names.OwnerName
import com.github.choppythelumberjack.trivialgen.util._

/**
  * Turns a Scala [[Type]] into a string. Override this if you want to change how types are stringified
  * globally instead of just in specific instances.
  */
trait RawTypeBuilder {
  def apply(scalaType: Type): String
}

class DefaultRawTypeBuilder extends RawTypeBuilder {
  /**
    * Stringifies the full name of the current type.
    *
    * Override this definition if you need to change how the T part of a raw type T[A, B, ...] is stringified.
    */
  def fullName(ownerName: Option[OwnerName], typeName: String): String = TypeUtil.names.concat(ownerName, typeName)

  /**
    * Stringifies each type argument of the current type.
    *
    * Override this definition if you need to change how the A, B, ... parts of a raw type T[A, B, ...]
    * are stringified.
    *
    * @param typeArgs Regularly empty for types without type arguments.
    */
  def rawTypeArgs(typeArgs: Seq[Type]): Seq[String] = typeArgs.map(this.apply)

  override def apply(t: Type): String = {
    val symbol = t.symbolPreserveAliases
    val ownerName = TypeUtil.names.ownerName(symbol)
    val typeName = symbol.name.toString

    // Now we have to treat type arguments:
    //   1. Type arguments aren't provided by fullName (obviously), but we still need them in the
    //      raw string representation of the type. So for example, we need to turn a scala.Array
    //      into a scala.Array[java.lang.String] for a type Array[String].
    //   2. To accommodate type arguments that have type arguments themselves and any potential
    //      overriding raw type builders (e.g. a raw type builder that simplifies packages based
    //      on imports) we simply call this apply function recursively.
    val args = rawTypeArgs(t.typeArgs)

    fullName(ownerName, typeName) + (if (args.nonEmpty) s"[${args.mkString(", ")}]" else "")
  }
}

/**
  * Simplifies names of all imported types and packages.
  *
  * Considers "java.lang._", "scala._" and "scala.Predef._" by default, since Scala imports these
  * namespaces by default.
  */
class ImportSimplifyingRawTypeBuilder(imports: Set[Import]) extends DefaultRawTypeBuilder {
  val classes: Set[String] =
    imports.filter(_.isInstanceOf[Import.Entity]).map { case e: Import.Entity => e.tpe.symbolPreserveAliases.fullName }
  val packages: Set[String] =
    imports.filter(_.isInstanceOf[Import.Package]).map { case p: Import.Package => p.name } +
      "java.lang" + "scala" + "scala.Predef"

  /**
    * @return The shortest version of the owner name possible based on imported packages.
    */
  private def simplifyOwnerName(ownerName: OwnerName): Option[OwnerName] = {
    // The owner name has to start with the full package, because an import of a package is basically package._, so
    // we can't, for example, take a substring of the package to simplify the owner name.
    (packages.filter(p => ownerName.startsWith(p))
      .map(p => ownerName.drop(p.length))
      .map(n => if (n.startsWith(".")) n.drop(1) else n) + ownerName)
      .minBy(_.length) // Take the shortest owner name.
      .emptyToNone
  }

  override def fullName(ownerName: Option[OwnerName], typeName: String): String = {
    val fullName = super.fullName(ownerName, typeName)
    if (classes.contains(fullName)) {
      typeName
    } else {
      super.fullName(ownerName.flatMap(simplifyOwnerName), typeName)
    }
  }
}
