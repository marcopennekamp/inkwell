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

    val over18 = ctx.run {
      query[Person].filter(p => age(p.birthday) >= 18)
    }
    over18.map(_.firstName) should (have length 1 and contain ("Rick"))

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
