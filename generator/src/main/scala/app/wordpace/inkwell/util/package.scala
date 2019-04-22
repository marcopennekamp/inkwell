package app.wordpace.inkwell

import java.sql.ResultSet

package object util {

  implicit class ResultSetExtensions(resultSet: ResultSet) {
    def toIterator: Iterator[ResultSet] = new Iterator[ResultSet] {
      override def hasNext: Boolean = resultSet.next()
      override def next(): ResultSet = resultSet
    }
  }

  implicit class StringExtensions(str: String) {
    def emptyToNone: Option[String] = if (str.nonEmpty) Some(str) else None
  }

}
