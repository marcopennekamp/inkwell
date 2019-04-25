package app.wordpace.inkwell.integration

import java.time.LocalDateTime

import io.getquill._
import org.scalatest.{FlatSpec, Matchers, OptionValues}
import plumbus.academy.{Id, Person, PersonSkill, Skill}
import Util.GroupByExtensions

class SchemaTests extends FlatSpec with Matchers with OptionValues with Schemas {

  "Plumbus Academy Schema" should "be queryable with the generated code" in {
    val ctx = new H2JdbcContext[SnakeCase](SnakeCase, plumbusAcademyDataSource)
    import ctx._

    // TODO: Add this as an extended trait PersonQuotes to Person's companion object.
    val age = quote((date: LocalDateTime) => infix"DATEDIFF(YEAR, $date, CURRENT_DATE)".as[Int])

    // Test general accessibility and querying. This ensures that our generated code works properly together
    // with Quill. Also ensures that Person.title is an Option.
    val over18 = ctx.run {
      query[Person].filter(p => age(p.birthday) >= 18)
    }
    over18 should have length 1
    val rick = over18.head
    rick.firstName shouldEqual "Rick" // Check that we've really gotten Rick here (i.e. the age check works).
    rick.title shouldEqual Some("Dr.") // Check that the nullable title column is a Some in Rick's case.
    assert(rick.id.isInstanceOf[Id[Person]]) // Check that we are really working with Id[A] types.

    // Test a more complex query with implicit joins. Also ensures that Person.title is an Option.
    case class SkillWithLevel(skill: Skill, level: Int)
    case class SkilledPerson(person: Person, skills: Seq[SkillWithLevel])

    val skills = ctx.run {
      for {
        p <- query[Person] if age(p.birthday) < 18
        ps <- query[PersonSkill] if ps.personId == p.id
        s <- query[Skill] if s.id == ps.skillId
      } yield (p, s, ps)
    }.map { case (p, s, ps) => (p, SkillWithLevel(s, ps.level)) }
     .combine.map(SkilledPerson.tupled)

    val morty = skills.find(_.person.firstName == "Morty").value
    morty.person.firstName shouldEqual "Morty" // Check that we've really gotten Morty here (i.e. the age check works).
    morty.person.title shouldEqual None // Check that the nullable title column is a None in Morty's case.
    morty.skills should have length 3
    val martialArts = morty.skills.find(_.skill.name == "Martial Arts").value
    martialArts.level shouldEqual 3
  }

}