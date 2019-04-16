package com.github.choppythelumberjack.trivialgen.generator

import com.github.choppythelumberjack.trivialgen.schema.Table

import scala.reflect.runtime.universe.Type

/**
  * [[TableInheritances]] holds all inheritance declarations (extends, with) for a single table. Please
  * take care to only provide one proper class to extend (although failing to adhere to this will only
  * result in generated code that does not compile).
  *
  * [[fullNames]] can be used for types which don't have a valid representation at generator runtime.
  * For example, we can use [[fullNames]] to support <b>extension methods:</b>
  *
  *   Say we generate a case class `Person` with a property `birthday`. We want to add an `age`
  *   function to the class, which calculates the current age from the birthday property. Instead
  *   of (unsafely) generating the method into the case class, we can extend the case class with a
  *   user-defined trait:
  *   {{{
  *     case class Person(..., birthday: LocalDateTime) extends PersonFunctions
  *   }}}
  *
  *   But how do we define the trait so that it can access the values from `Person`? Like so:
  *   {{{
  *     trait PersonFunctions { self: Person =>
  *       def age: Long = self.birthday.until(LocalDateTime.now(), ChronoUnit.DAYS)
  *     }
  *   }}}
  *
  *   Only problem: We can't reference PersonFunctions in the generator project as a [[Type]], because
  *   it needs the Person case class to even compile. So [[TableInheritances]] allows you to pass a
  *   String instead of a Type for this and other use cases.
  */
case class TableInheritances(
  types: Seq[Type] = Seq.empty,
  fullNames: Seq[String] = Seq.empty
)

object TableInheritances {
  val empty = TableInheritances()
}

/**
  * [[SchemaInheritances]] holds all inheritance declarations (extends, with) for the whole schema.
  *
  * Note that the inheritance map uses SQL names as keys and <b>not</b> Scala names. This is because
  * SQL names are stable while Scala names depend on the [[NamingStrategy]].
  */
case class SchemaInheritances(map: Map[Table.Name, TableInheritances] = Map.empty) {
  def get(name: Table.Name): TableInheritances = map.getOrElse(name, TableInheritances.empty)
}

object SchemaInheritances {
  val empty = SchemaInheritances()
}
