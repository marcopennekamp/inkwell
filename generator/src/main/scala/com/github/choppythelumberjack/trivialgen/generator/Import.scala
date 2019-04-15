package com.github.choppythelumberjack.trivialgen.generator

import scala.reflect.runtime.universe.Type

sealed trait Import

object Import {
  case class Entity(tpe: Type) extends Import
  case class Package(name: String) extends Import
}
