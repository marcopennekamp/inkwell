package com.github.choppythelumberjack.trivialgen.generator

import com.github.choppythelumberjack.trivialgen.generator.TypeUtil.TypeExtensions

import scala.reflect.runtime.universe.Type

sealed trait Import

object Import {
  case class Entity(tpe: Type) extends Import {
    def fullName: String = tpe.symbolPreserveAliases.fullName
  }
  case class Package(name: String) extends Import
}
