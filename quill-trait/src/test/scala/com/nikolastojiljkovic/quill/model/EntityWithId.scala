package com.nikolastojiljkovic.quill.model

import com.nikolastojiljkovic.annotation.Field

trait EntityWithId {
  @Field("id")
  def id: Option[Int]
}