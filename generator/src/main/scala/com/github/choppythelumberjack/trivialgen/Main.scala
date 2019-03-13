package com.github.choppythelumberjack.trivialgen

import reflect.io
import scala.reflect.io.Path

object Main {
  def main(args:Array[String]):Unit = {
    val outputPath = args(0)
    val content =
      """
        |package foo.bar.baz
        |
        |object Foo {
        |  def main(args:Array[String]):Unit = {
        |    println("bar")
        |  }
        |}
      """.stripMargin
    val newPath = outputPath + "/foo/bar/baz/"
    println(s"========== Creating Path if not exist: ${newPath} ==========") // if not exist sto
    new java.io.File(newPath).mkdirs()

    val filePath = new java.io.File(newPath, "Foo.scala")
    println(s"========== Creating File in Path: ${filePath.getPath} ==========")
    io.File(Path(filePath)).writeAll(content)
  }
}
