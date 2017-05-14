package com.nikolastojiljkovic.quilltrait.model

import com.nikolastojiljkovic.annotation.{ Embedded, Field }

trait SampleTraitWithNestable {
  @Embedded
  def options1: SampleEmbedded1

  //  @Embedded
  //  def options2: Option[SampleEmbedded2]
}

case class SampleEmbedded1(
  @Field("p01") p01: String
)

case class SampleEmbedded2(
  @Field("p02") p02: String,

  @Field("p03") p03: Option[String]
)