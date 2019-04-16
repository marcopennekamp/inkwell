package com.github.choppythelumberjack.trivialgen.generator

import com.github.choppythelumberjack.trivialgen.util._

import scala.reflect.runtime.universe._

object TypeUtil {

  implicit class TypeExtensions(t: Type) {
    /**
      * Due to an anomaly with the type API, the standard typeSymbol function returns a dealiased symbol. This
      * function returns the proper symbol without dereferencing a potential alias.
      *
      * @return The symbol of the type, preserving aliases.
      *
      * @see https://stackoverflow.com/questions/12718436/how-to-determine-a-type-alias-with-scala-reflection
      */
    def symbolPreserveAliases: Symbol = {
      // t.typeSymbol automatically dereferences aliases:
      //   https://stackoverflow.com/questions/12718436/how-to-determine-a-type-alias-with-scala-reflection
      // We want to preserve aliases, however, since it is probably the user's intention when specifying an
      // alias as the data type for a column. Thus, as suggested in the StackOverflow answer, we cast the type
      // to the internal representation and use typeSymbolDirect (which does not dealias).
      t.asInstanceOf[scala.reflect.internal.Types#Type].typeSymbolDirect.asInstanceOf[Symbol]
    }
  }

  object names {
    type OwnerName = String

    /**
      * An owner name may be None, which means that the symbol for which an owner was sought has no owner.
      * This covers the edge case where no base package has been defined for some classes in a project and
      * is also used by [[ImportSimplifyingRawTypeBuilder]] to suggest that "no owner" is needed to
      * represent the current symbol.
      *
      * @return An owner name and a type name.
      */
    def split(fullName: String): (Option[OwnerName], String) = {
      // We cannot use symbol.owner.fullName, because fullName drops package objects, but only if the current
      // object is not a package object. Hence, we generate the fullName of the symbol itself and extract
      // the owner name through textual methods.
      val parts = fullName.split('.')
      (parts.init.mkString(".").emptyToNone, parts.last)
    }

    /**
      * @return A concatenation of the owner name and the type name.
      */
    def concat(ownerName: Option[OwnerName], typeName: String): String = {
      Seq(ownerName, Some(typeName)).flatten.mkString(".")
    }
  }

}
