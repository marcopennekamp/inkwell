package app.wordpace.inkwell.test

import app.wordpace.inkwell.generator.{CamelCaseToSnakeCase, SnakeCaseToCamelCase}
import app.wordpace.inkwell.schema.{Column, Table}
import org.scalatest.{FlatSpec, Matchers}

import scala.reflect.runtime.universe.typeOf

class NamingStrategySpec extends FlatSpec with Matchers {

  // TODO: Test a naming strategy which operates on some meaningful Column property (such as isNullable).

  // The null isn't safe, these objects aren't safely initialised either, but it's sufficient for the current purpose.
  implicit def mockColumn(name: String): Column = Column(name, typeOf[Int], isNullable = false, null)
  implicit def mockTable(name: String): Table = Table(name, Seq.empty, Seq.empty, null)

  "CamelCaseToSnakeCase" should "translate camel case to snake case" in {
    val st = CamelCaseToSnakeCase
    st.property("camelCase") shouldEqual "camel_case"
    st.model("camelCase") shouldEqual "camel_case"
    st.property("CamelCase") shouldEqual "camel_case"
    st.model("CamelCase") shouldEqual "camel_case"
    st.property("camelcase") shouldEqual "camelcase"
    st.model("camelcase") shouldEqual "camelcase"
    st.property("snake_case") shouldEqual "snake_case"
    st.model("snake_case") shouldEqual "snake_case"
    st.property("MicRoTraNsaCtIonS") shouldEqual "mic_ro_tra_nsa_ct_ion_s"
    st.model("MicRoTraNsaCtIonS") shouldEqual "mic_ro_tra_nsa_ct_ion_s"
  }

  "SnakeCaseToCamelCase" should "translate snake case to camel case" in {
    val st = SnakeCaseToCamelCase
    st.property("snake_case") shouldEqual "snakeCase"
    st.model("snake_case") shouldEqual "SnakeCase"
    st.property("SNAKE_CASE") shouldEqual "snakeCase"
    st.model("SNAKE_CASE") shouldEqual "SnakeCase"
    st.property("snakecase") shouldEqual "snakecase"
    st.model("snakecase") shouldEqual "Snakecase"
    st.property("mic_ro_tra_nsa_ct_ion_s") shouldEqual "micRoTraNsaCtIonS"
    st.model("mic_ro_tra_nsa_ct_ion_s") shouldEqual "MicRoTraNsaCtIonS"
  }

}
