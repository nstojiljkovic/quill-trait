package com.nikolastojiljkovic.quilltrait

import com.nikolastojiljkovic.quilltrait.model.{ Page, SampleEmbedded1, SampleTraitWithNestable, SeoSupport }

trait PageSpec extends Spec {

  val testEntries = List(
    new Page(Some(1), isRoot = true, None, Some(0), "Root", "/") with SeoSupport with SampleTraitWithNestable {
      val sitemapChangeFrequency = "monthly"
      val sitemapPriority = 0.00
      val options1 = SampleEmbedded1("t1")
      val options2 = None
    },
    new Page(Some(2), isRoot = false, Some(1), Some(0), "Home page", "home") with SeoSupport with SampleTraitWithNestable {
      val sitemapChangeFrequency = "hourly"
      val sitemapPriority = 1.0
      val options1 = SampleEmbedded1("p1")
      // val options2 = Some(SampleEmbedded2("p2", None))
      val options2 = None
    },
    new Page(Some(3), isRoot = false, Some(2), Some(0), "Child page 1", "child1") with SeoSupport with SampleTraitWithNestable {
      val sitemapChangeFrequency = "weekly"
      val sitemapPriority = 0.7
      val options1 = SampleEmbedded1("z")
      val options2 = None
    },
    new Page(Some(4), isRoot = false, Some(2), Some(1), "Child page 2", "child2") with SeoSupport with SampleTraitWithNestable {
      val sitemapChangeFrequency = "weekly"
      val sitemapPriority = 0.7
      val options1 = SampleEmbedded1("x1")
      // val options2 = Some(SampleEmbedded2("x2", Some("x3")))
      val options2 = None
    }
  )
}
