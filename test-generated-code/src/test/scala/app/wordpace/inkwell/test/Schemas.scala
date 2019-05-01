package app.wordpace.inkwell.test

import java.io.Closeable

import javax.sql.DataSource
import org.h2.jdbcx.JdbcDataSource

trait Schemas {
  def makeDataSource(sqlScriptName: String): DataSource with Closeable = {
    val config = ConfigLoader.databaseConfiguration(sqlScriptName, scriptRoot = "../")
    val ds = new JdbcDataSource()
    ds.setURL(config.url)
    ds.setUser(config.username)
    ds.setPassword(config.password)
    ds.asInstanceOf[DataSource with Closeable]
  }

  def plumbusAcademyDataSource: DataSource with Closeable = makeDataSource("plumbus_academy.sql")
}
