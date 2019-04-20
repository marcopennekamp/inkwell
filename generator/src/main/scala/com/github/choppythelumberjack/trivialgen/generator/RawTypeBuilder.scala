package com.github.choppythelumberjack.trivialgen.generator

import com.github.choppythelumberjack.trivialgen.generator.TypeUtil.TypeExtensions
import com.github.choppythelumberjack.trivialgen.generator.TypeUtil.names.OwnerName
import com.github.choppythelumberjack.trivialgen.util._

import scala.reflect.runtime.universe._

/**
  * Turns a Scala [[Type]] into a String. Override this if you want to change how types are stringified
  * globally instead of just in specific instances.
  */
trait RawTypeBuilder {
  def apply(scalaType: Type): String

  /**
    * Use this in cases where a [[Type]] can't be resolved at generator runtime. Does not support type
    * arguments (yet).
    *
    * Used by [[DefaultModelEmitter]] to process [[TableInheritances]].
    */
  def apply(fullName: String): String
}

// TODO: Consider renaming RawTypeBuilder to TypeStringifier or even TypeEmitter.

class DefaultRawTypeBuilder extends RawTypeBuilder {
  /**
    * Stringifies the full name of the current type.
    *
    * Override this definition if you need to change how the T part of a raw type T[A, B, ...] is stringified.
    */
  protected def fullName(ownerName: Option[OwnerName], typeName: String): String = TypeUtil.names.concat(ownerName, typeName)

  /**
    * Stringifies each type argument of the current type.
    *
    * Override this definition if you need to change how the A, B, ... parts of a raw type T[A, B, ...]
    * are stringified.
    *
    * @param typeArgs Regularly empty for types without type arguments.
    */
  protected def rawTypeArgs(typeArgs: Seq[Type]): Seq[String] = typeArgs.map(this.apply)

  protected def transform(fullName: String, typeArgs: Seq[Type]): String = {
    val (ownerName, typeName) = TypeUtil.names.split(fullName)

    // Now we have to treat type arguments:
    //   1. Type arguments aren't provided by fullName (obviously), but we still need them in the
    //      raw string representation of the type. So for example, we need to turn a scala.Array
    //      into a scala.Array[java.lang.String] for a type Array[String].
    //   2. To accommodate type arguments that have type arguments themselves and any potential
    //      overriding raw type builders (e.g. a raw type builder that simplifies packages based
    //      on imports) we simply call this apply function recursively.
    val args = rawTypeArgs(typeArgs)

    this.fullName(ownerName, typeName) + (if (args.nonEmpty) s"[${args.mkString(", ")}]" else "")
  }

  override def apply(t: Type): String = transform(t.symbolPreserveAliases.fullName, t.typeArgs)

  // TODO: Add support for type arguments?
  override def apply(fullName: String): String = transform(fullName, Seq.empty)
}

/**
  * Simplifies names of all imported types and packages.
  *
  * Considers "java.lang._", "scala._" and "scala.Predef._" by default, since Scala imports these
  * namespaces by default.
  */
class ImportSimplifyingRawTypeBuilder(imports: Set[Import]) extends DefaultRawTypeBuilder {
  protected val classes: Set[String] = imports.flatMap { case e: Import.Entity => Some(e.fullName); case _ => None }
  protected val packages: Set[String] = imports.flatMap { case p: Import.Package => Some(p.name); case _ => None } ++
      Set("java.lang", "scala", "scala.Predef")

  // TODO: Include the base package of the generated source file in packages, since it's also "imported" by default.
  //       This would depend on the package of the current compilation unit (see SchemaEmitter), so this is not as
  //       trivial to implement.

  /**
    * @return The shortest version of the owner name possible based on imported packages.
    */
  protected def simplifyOwnerName(ownerName: OwnerName): Option[OwnerName] = {
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
