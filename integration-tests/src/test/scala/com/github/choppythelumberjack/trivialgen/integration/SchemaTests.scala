package com.github.choppythelumberjack.trivialgen.integration

import java.time.LocalDateTime

import io.getquill._
import org.scalatest.{FlatSpec, Matchers, OptionValues}
import plumbus.academy.{Person, PersonSkill, Skill}

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

    case class SkillWithLevel(skill: Skill, level: Int)

    val skills = ctx.run {
      for {
        p <- query[Person] if age(p.birthday) < 18
        ps <- query[PersonSkill] if ps.personId == p.id
        s <- query[Skill] if s.id == ps.skillId
      } yield (p, s, ps)
    }.map { case (p, s, ps) => (p, SkillWithLevel(s, ps.level)) }
     .groupBy(_._1).mapValues(_.map(_._2))

    skills.keySet.map(_.firstName) should contain ("Morty")
    val (_, mortySkills) = skills.find(_._1.firstName == "Morty").value
    mortySkills should have length 3
    val martialArts = mortySkills.find(_.skill.name == "Martial Arts").value
    martialArts.level shouldEqual 3
  }

}
