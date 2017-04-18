package com.nikolastojiljkovic.quill

import io.getquill.dsl.{ MetaDsl, MetaDslLowPriorityImplicits }

import scala.language.experimental.macros

trait AnnotatedTraitSupportMetaDslHighPriorityImplicits extends MetaDslLowPriorityImplicits {
  this: MetaDsl =>

  implicit def materializeRefinedTypeQueryMeta[T]: QueryMeta[T] = macro AnnotatedTraitSupportBlackboxMacro.materializeQueryMeta[T]
  implicit def materializeRefinedTypeUpdateMeta[T]: UpdateMeta[T] = macro AnnotatedTraitSupportWhiteboxMacro.materializeUpdateMeta[T]
  implicit def materializeRefinedTypeInsertMeta[T]: InsertMeta[T] = macro AnnotatedTraitSupportWhiteboxMacro.materializeInsertMeta[T]
  implicit def materializeRefinedTypeSchemaMeta[T]: SchemaMeta[T] = macro AnnotatedTraitSupportWhiteboxMacro.materializeSchemaMeta[T]

}

trait AnnotatedTraitSupport extends AnnotatedTraitSupportMetaDslHighPriorityImplicits {
  this: MetaDsl =>

}
