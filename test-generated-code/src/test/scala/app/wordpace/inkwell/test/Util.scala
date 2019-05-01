package app.wordpace.inkwell.test

object Util {
  implicit class GroupByExtensions[A, B](seq: Seq[(A, B)]) {
    /**
      * Combine results from a database operation returning multiple row that are intended
      * to be <b>combined</b> into a list.
      *
      * Technical explanation: Creates a list of tuples (A, Seq[B]) with all tuples of value (a, b_i)
      * grouped together as (a, Seq(b_0, b_1, ...).
      */
    def combine: Seq[(A, Seq[B])] = seq.groupBy(_._1).map { case (key, tuples) => (key, tuples.map(_._2)) }.toSeq
  }
}
