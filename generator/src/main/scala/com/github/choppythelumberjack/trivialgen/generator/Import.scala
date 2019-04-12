package com.github.choppythelumberjack.trivialgen.generator

import scala.reflect.ClassTag

sealed trait Import

object Import {
  case class Entity(classTag: ClassTag[_]) extends AnyVal with Import
  case class Package(name: String) extends Import
}
