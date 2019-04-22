package app.wordpace.inkwell.integration

import java.io.Closeable

import javax.sql.DataSource
import org.h2.jdbcx.JdbcDataSource

trait Schemas {
  def makeDataSource(sqlScriptName: String): DataSource with Closeable = {
    val ds = new JdbcDataSource()
    ds.setURL(s"jdbc:h2:mem:sample;INIT=RUNSCRIPT FROM '../generator/src/test/resources/$sqlScriptName'")
    ds.setUser("sa")
    ds.setPassword("")
    ds.asInstanceOf[DataSource with Closeable]
  }

  def plumbusAcademyDataSource: DataSource with Closeable = makeDataSource("plumbus_academy.sql")
}
