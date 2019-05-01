package app.wordpace.inkwell.generator

import scala.reflect.runtime.universe.Type

sealed trait Import

object Import {
  /**
    * A single entity to import with `import fullName`. "Entities" are mostly types, but may also be objects
    * or any other named entity which can be imported.
    *
    * If possible, use `Entity(typeOf[A])` to construct this case class, since typeOf gives certain compile-time
    * guarantees and is also resilient against refactoring.
    */
  case class Entity(fullName: String) extends Import
  object Entity {
    def apply(t: Type): Entity = Entity(ScalaTypeReference(t).fullName)
  }

  /**
    * A wildcard import (`import name._`) from any named package or object.
    */
  case class Wildcard(name: String) extends Import
}
