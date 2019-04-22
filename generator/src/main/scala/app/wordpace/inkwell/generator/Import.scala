package app.wordpace.inkwell.generator

import app.wordpace.inkwell.generator.TypeUtil.TypeExtensions

import scala.reflect.runtime.universe.Type

sealed trait Import

object Import {
  /**
    * A single type to import with `import fullName`.
    *
    * If possible (see [[TableInheritances]] for an example case in which it is not possible to refer to
    * a type via typeOf at generator runtime), use Entity.fromType to construct this case class, since typeOf
    * gives certain compile-time guarantees and is also resilient against refactoring.
    */
  case class Entity(fullName: String) extends Import
  object Entity {
    def fromType(tpe: Type): Entity = Entity(tpe.symbolPreserveAliases.fullName)
  }

  /**
    * A package to import with `import name._`.
    */
  case class Package(name: String) extends Import
}
