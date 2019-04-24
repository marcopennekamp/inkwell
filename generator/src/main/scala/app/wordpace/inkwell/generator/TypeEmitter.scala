package app.wordpace.inkwell.generator

import app.wordpace.inkwell.GeneratorConfiguration
import app.wordpace.inkwell.GeneratorConfiguration
import app.wordpace.inkwell.generator.TypeUtil.TypeExtensions
import app.wordpace.inkwell.generator.TypeUtil.names.OwnerName
import app.wordpace.inkwell.schema.Column
import app.wordpace.inkwell.schema.TypeResolver
import app.wordpace.inkwell.util._

import scala.reflect.runtime.universe._

/**
  * Provides various ways to turn a scala type into a string.
  *
  * The plugins are defined so that you can mix in proper implementations at your leisure.
  */
trait TypeEmitter extends TypeEmitter.ColumnPlugin {
  /**
    * Turns a scala type into a raw type string.
    */
  def fromType(scalaType: Type): String

  // TODO: Add support for type arguments?
  /**
    * Turns a type name into a raw type string.
    *
    * Use this in cases where a [[Type]] can't be resolved at generator runtime. Does not support
    * type arguments (yet).
    *
    * Used by [[DefaultModelEmitter]] to process [[TableInheritances]].
    */
  def fromName(fullName: String): String

  // Provide simple default implementations for all plugins,
  override def fromColumn(column: Column): String = fromType(column.scalaType)
}

object TypeEmitter {
  trait ColumnPlugin {
    /**
      * Emits a raw type string for the specified column.
      *
      * For example, let's say the application (using this code generator library) defines an `Id[A]` type,
      * which represents (database) IDs for any type `A`. Say you have a table `person` from which a case class
      * `Person` is generated. You can resolve the JDBC type of its ID column to `Id` with [[TypeResolver]], but
      * you can not provide the type argument `A = Person` since `Person` does not exist until code generation is
      * finished. In such a case, fromColumn can be used to emit the right type for the property.
      */
    def fromColumn(column: Column): String
  }
}

class DefaultTypeEmitter extends TypeEmitter {
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
  protected def rawTypeArgs(typeArgs: Seq[Type]): Seq[String] = typeArgs.map(this.fromType)

  /**
    * Transforms the full name and the type arguments into the raw type string.
    */
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

  override def fromType(t: Type): String = transform(t.symbolPreserveAliases.fullName, t.typeArgs)
  override def fromName(fullName: String): String = transform(fullName, Seq.empty)
}

/**
  * Simplifies the names of all imported types and packages.
  *
  * Simplifies "java.lang._", "scala._" and "scala.Predef._" by default, since Scala imports these
  * namespaces by default.
  */
class ImportSimplifyingTypeEmitter(imports: Set[Import]) extends DefaultTypeEmitter {
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
    // The owner name has to start with the full package, because an import of a package is basically
    // package._, so we can't, for example, take a substring of the package to simplify the owner name.
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

/**
  * This type emitter plugin resolves primary and foreign key columns to the correct raw type:
  *
  *   (1) A single-column primary key of a type `A` is resolved to a type `id(A)`, where `id` is an overridable
  *       function which defaults to `Id[A]`. For example, for a table `person` with a primary key column `id`,
  *       a property `id: Id[Person]` is emitted.
  *   (2) A foreign key pointing to a single-column primary or unique key (see [[Column.references]]) is resolved
  *       to the raw type of the referenced column (as specified by its [[PropertyEmitter]]). For example, for a
  *       table `employees` with a foreign key column `person_id`, a property `personId: Id[Person]` is emitted,
  *       using the type `Id[Person]` of the referenced column.
  *
  * For a primary key that is also a foreign key, case (2) prevails, since the primary key is then interpreted
  * as the primary key of another table, just replicated here due to a one-to-one relationship. (This is valid
  * in a few edge cases where the original table can't or shouldn't be manipulated to add more columns.)
  *
  * <b>Usage Note:</b> The default implementation of `id(A)` requires you to import any type Id[A] (via
  * [[GeneratorConfiguration.imports]]). For Quill support, it must either extend AnyVal (which should suffice
  * in most use cases) or come with an encoder and decoder.
  *
  * In any case, this emitter only handles single-column primary and foreign keys. You will have to implement
  * some individual handling if you want to type multi-column keys.
  */
trait KeyAsIdColumnPlugin extends TypeEmitter.ColumnPlugin { self: TypeEmitter =>
  protected def config: GeneratorConfiguration
  protected def id(modelName: String): String = s"Id[$modelName]"

  override def fromColumn(column: Column): String = {
    val table = column.table

    // Handle case (2) before case (1), because case (2) prevails when both are true, as described above.
    if (column.references.length == 1) {
      // Case (2): The column is a foreign key referencing exactly one unique key.
      self.fromColumn(column.references.head)
    } else if (table.primaryKey == Seq(column)) {
      // Case (1): The column is the only primary key of its table.
      id(table.scalaName(config.namingStrategy))
    } else {
      self.fromType(column.scalaType)
    }
  }
}
