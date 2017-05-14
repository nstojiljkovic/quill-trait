package com.nikolastojiljkovic.quilltrait

import com.nikolastojiljkovic.quilltrait.model.{ Page, SampleTraitWithNestable, SeoSupport }
import io.getquill.context.sql.SqlContext

trait PageSqlSpec extends PageSpec {

  implicit val context: SqlContext[_, _] with AnnotatedTraitSupport

  import context._

  val `Ex 1` =
    quote {
      for {
        c <- query[Page with SeoSupport with SampleTraitWithNestable]
        p <- query[Page with SeoSupport with SampleTraitWithNestable] if p.id == c.parentId
      } yield p.id
    }
  val `Ex 1 expected result` = List(Some(1), Some(2), Some(2))

  val `Ex 2` =
    quote {
      for {
        c <- query[Page with SeoSupport with SampleTraitWithNestable].sortBy(p => p.id)
      } yield c
    }
  val `Ex 2 expected result` = testEntries
}
