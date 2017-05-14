package com.nikolastojiljkovic.quilltrait.model

import com.nikolastojiljkovic.annotation.Field

trait EntityWithId {
  @Field("id")
  def id: Option[Int]
}