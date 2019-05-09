package app.wordpace.inkwell.generator

import app.wordpace.inkwell.generator.TypeUtil.names.OwnerName
import app.wordpace.inkwell.util._

/**
  * Turns a type reference into a string.
  */
trait TypeEmitter {
  /**
    * Turns a type reference into a raw type string. The context can be used to make additional decisions
    * based on the
    */
  def apply(ref: TypeReference)(implicit context: CompilationUnit): String
}

class DefaultTypeEmitter extends TypeEmitter {
  /**
    * Stringifies the full name of the current type.
    *
    * Override this definition if you need to change how the T part of a raw type T[A, B, ...] is stringified.
    */
  protected def stringifyFullName(ownerName: Option[OwnerName], typeName: String)(implicit context: CompilationUnit): String = {
    TypeUtil.names.concat(ownerName, typeName)
  }

  /**
    * Stringifies each type argument of the current type.
    *
    * Override this definition if you need to change how the A, B, ... parts of a raw type T[A, B, ...]
    * are stringified.
    *
    * @param typeArguments Most of the time, the sequence is empty for types without type arguments, so don't
    *                      assume the sequence has elements!
    */
  protected def stringifyTypeArguments(typeArguments: Seq[TypeReference])(implicit context: CompilationUnit): Seq[String] = {
    typeArguments.map(apply)
  }

  override def apply(ref: TypeReference)(implicit context: CompilationUnit): String = {
    val (ownerName, typeName) = TypeUtil.names.split(ref.fullName)

    // Now we have to treat type arguments:
    //   1. Type arguments aren't provided by fullName (obviously), but we still need them in the
    //      raw string representation of the type. So for example, we need to turn a scala.Array
    //      into a scala.Array[java.lang.String] for a type Array[String].
    //   2. To accommodate type arguments that have type arguments themselves and any potential
    //      overriding type emitters (e.g. our type emitter that simplifies packages based on
    //      imports) we simply call apply recursively.
    val args = stringifyTypeArguments(ref.typeArguments)

    this.stringifyFullName(ownerName, typeName) + (if (args.nonEmpty) s"[${args.mkString(", ")}]" else "")
  }
}

/**
  * Simplifies the names of all imported types and packages.
  *
  * Simplifies "java.lang._", "scala._", "scala.Predef._" and the current compilation unit's package by default,
  * since Scala imports these namespaces by default.
  */
class ImportSimplifyingTypeEmitter extends DefaultTypeEmitter {
  protected def classes(implicit context: CompilationUnit): Set[String] = {
    context.imports.flatMap { case e: Import.Entity => Some(e.fullName); case _ => None }
  }
  protected def wildcards(implicit context: CompilationUnit): Set[String] = {
    context.imports.flatMap { case w: Import.Wildcard => Some(w.name); case _ => None } ++
      Set("java.lang", "scala", "scala.Predef", context.packageName)
  }

  /**
    * @return The shortest version of the owner name possible based on imported packages.
    */
  protected def simplifyOwnerName(ownerName: OwnerName)(implicit context: CompilationUnit): Option[OwnerName] = {
    // The owner name has to start with the full package name, because an import of a wildcard is basically
    // package._, so we can't, for example, take a substring of the package to simplify the owner name.
    (wildcards.filter(w => ownerName.startsWith(w))
      .map(w => ownerName.drop(w.length))
      .map(n => if (n.startsWith(".")) n.drop(1) else n) + ownerName)
      .minBy(_.length) // Take the shortest owner name.
      .emptyToNone
  }

  override def stringifyFullName(ownerName: Option[OwnerName], typeName: String)(implicit context: CompilationUnit): String = {
    val fullName = super.stringifyFullName(ownerName, typeName)
    if (classes.contains(fullName)) {
      typeName
    } else {
      super.stringifyFullName(ownerName.flatMap(simplifyOwnerName), typeName)
    }
  }
}
