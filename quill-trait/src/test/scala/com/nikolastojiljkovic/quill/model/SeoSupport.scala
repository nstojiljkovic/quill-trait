package com.nikolastojiljkovic.quill.model

import com.nikolastojiljkovic.annotation.Field

trait SeoSupport extends EntityWithId {
  this: Page =>

  @Field("sitemap_change_freq")
  def sitemapChangeFrequency: String

  @Field("sitemap_priority")
  def sitemapPriority: Double
}
