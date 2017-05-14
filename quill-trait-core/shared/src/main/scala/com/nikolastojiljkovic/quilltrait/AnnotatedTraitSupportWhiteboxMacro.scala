package com.nikolastojiljkovic.quilltrait

import scala.annotation.compileTimeOnly
import scala.reflect.macros.whitebox.{ Context => MacroContext }

@compileTimeOnly("AnnotatedTraitSupportWhiteboxMacro is used during macro expansion only")
class AnnotatedTraitSupportWhiteboxMacro(override val c: MacroContext) extends AnnotatedTraitSupportBlackboxMacro(c) {

}