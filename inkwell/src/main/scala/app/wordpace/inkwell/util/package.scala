package app.wordpace.inkwell

import java.io.File
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

    /**
      * Converts a package or full name to a file name.
      *
      * @example `"a.b.c".toFileName == "a/b/c"` (on Unix-based systems)
      */
    def toFileName: String = str.replace(".", File.separator)

    /**
      * Removes the last section of the string separated by `separator`.
      *
      * @example `"a.b.c".cutLast('.') == "a.b"`
      */
    def cutLast(separator: Char): String = str.split(separator).init.mkString(separator.toString)

    /**
      * Returns the package name of the given full name.
      */
    def cutPackageName: String = str.cutLast('.')
  }

}
