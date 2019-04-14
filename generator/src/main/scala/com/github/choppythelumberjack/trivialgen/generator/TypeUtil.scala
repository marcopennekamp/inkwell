package com.github.choppythelumberjack.trivialgen.generator

import scala.reflect.runtime.universe._

object TypeUtil {

  object names {
    /**
      * @return The full name of the symbol's owner or None if the symbol has no owner.
      */
    def ownerName(sym: Symbol): Option[String] = {
      // We cannot use sym.owner.fullName, because fullName drops package objects, but only if the current
      // object is not a package object. Hence, we generate the fullName of the symbol itself and extract
      // the owner name through textual methods.
      ownerName(sym.fullName)
    }

    /**
      * @return The full name of a symbol's owner based on the full name of the symbol or None if the symbol has no owner.
      */
    def ownerName(name: String): Option[String] = {
      val index = name.lastIndexOf('.')
      if (index >= 0) Some(name.substring(0, index)) else None
    }

    /**
      * @return A concatenation of the given names.
      */
    def concat(names: String*): String = names.mkString(".")

    /**
      * @return A concatenation of the given names, ignoring None values.
      */
    def concatOpt(names: Option[String]*): String = concat(names.flatten : _*)
  }

}
