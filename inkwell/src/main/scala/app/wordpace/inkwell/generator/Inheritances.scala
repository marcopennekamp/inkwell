package app.wordpace.inkwell.generator

import scala.reflect.runtime.universe.Type

/**
  * [[Inheritances]] holds all inheritance declarations (extends, with) for the whole schema. Please take
  * care to only provide one proper class to extend (although failing to adhere to this will only result
  * in generated code that does not compile).
  *
  * Note that the inheritance map should use Scala names. Although Scala names depend on the [[NamingStrategy]],
  * Scala names are closer to the actual "A extends B" declaration and thus easier to use in the context of
  * inheritance. Additionally, SQL names are usually case insensitive, which would complicate using SQL names.
  *
  * Inheritance declarations are based on [[TypeReference]] instances. [[NamedTypeReference]] can be used
  * for types which don't have a valid representation at generator runtime. For example, we can use it to
  * support <b>extension methods:</b>
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
  *   it needs the Person case class to even compile. So [[NamedTypeReference]] allows you to pass a
  *   String instead of a Type for this and other use cases.
  */
case class Inheritances(map: Map[String, Seq[TypeReference]]) {
  def get(name: String): Seq[TypeReference] = map.getOrElse(name, Seq.empty)
}

object Inheritances {
  val empty = Inheritances(Map.empty)
}
