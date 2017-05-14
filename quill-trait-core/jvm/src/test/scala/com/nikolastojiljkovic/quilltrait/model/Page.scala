package com.nikolastojiljkovic.quilltrait.model

import com.nikolastojiljkovic.annotation.{ Field, Table }

@Table("page")
case class Page(
  @Field("id") id: Option[Int],

  @Field("is_root") isRoot: Boolean,

  @Field("parent_id") parentId: Option[Int],

  @Field("sorting") sorting: Option[Int],

  @Field("title") title: String,

  @Field("path") path: String
) extends EntityWithId
