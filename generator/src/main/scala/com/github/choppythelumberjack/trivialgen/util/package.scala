package com.github.choppythelumberjack.trivialgen

import java.sql.ResultSet

package object util {

  implicit class ResultSetExtensions(resultSet: ResultSet) {
    def toIterator: Iterator[ResultSet] = new Iterator[ResultSet] {
      override def hasNext: Boolean = resultSet.next()
      override def next(): ResultSet = resultSet
    }
  }

}
