package com.nikolastojiljkovic.quilltrait

import com.nikolastojiljkovic.annotation.{ Embedded, Field, Table }
import io.getquill.{ MirrorContext, MirrorSqlDialect, Literal }

class SchemaMetaSpec extends Spec {

  @Table("simple")
  case class SimpleEntity(
    @Field("aa") a: String
  )

  @Table("test_entity_with_more_than_22_fields")
  case class TestEntityWith23Fields(
    @Field("aa_1") a1:   String,
    @Field("aa_2") a2:   String,
    @Field("aa_3") a3:   String,
    @Field("aa_4") a4:   String,
    @Field("aa_5") a5:   String,
    @Field("aa_6") a6:   String,
    @Field("aa_7") a7:   String,
    @Field("aa_8") a8:   String,
    @Field("aa_9") a9:   String,
    @Field("aa_10") a10: String,
    @Field("aa_11") a11: String,
    @Field("aa_12") a12: String,
    @Field("aa_13") a13: String,
    @Field("aa_14") a14: String,
    @Field("aa_15") a15: String,
    @Field("aa_16") a16: String,
    @Field("aa_17") a17: String,
    @Field("aa_18") a18: String,
    @Field("aa_19") a19: String,
    @Field("aa_20") a20: String,
    @Field("aa_21") a21: String,
    @Field("aa_22") a22: String,
    @Field("aa_23") a23: String
  )

  case class NestedEntity(
    @Field("ccc") c: String,

    @Field("eee") e: Option[String]
  )

  @Table("test_entity_custom_table_name")
  case class TestEntityWithNested(
    @Field("aaa") a: String,

    @Embedded b: NestedEntity
  )

  @Table("test_entity")
  case class TestEntityWithOptionalNested(
    @Field("aaa") a: String,

    @Embedded b: Option[NestedEntity]
  )

  trait SimpleEntityCopy {
    @Field("aa")
    def a: String
  }

  trait SimpleEntityTrait1 extends SimpleEntityCopy {
    this: SimpleEntity =>

    @Field("bb")
    def b: String
  }

  trait TestTraitWithNested extends SimpleEntityCopy {
    this: SimpleEntity =>

    @Field("bb")
    def b: String

    @Embedded
    def c: NestedEntity
  }

  trait TestTraitWithOptionalNested extends SimpleEntityCopy {
    this: SimpleEntity =>

    @Field("bb")
    def b: String

    @Embedded
    def c: Option[NestedEntity]
  }

  implicit object context extends MirrorContext[MirrorSqlDialect, Literal] with AnnotatedTraitSupport

  import context._

  "schema meta" - {
    "materialized schema meta for simple entity" in {
      val meta = materializeRefinedTypeSchemaMeta[SimpleEntity]
      val expectedEntity = quote {
        querySchema[SimpleEntity]("simple", _.a -> "aa")
      }
      meta.entity.toString mustEqual expectedEntity.toString
    }
    "materialized schema meta for simple entity with trait" in {
      val meta = materializeRefinedTypeSchemaMeta[SimpleEntity with SimpleEntityTrait1]
      val expectedEntity = quote {
        querySchema[SimpleEntity with SimpleEntityTrait1]("simple", _.a -> "aa", _.b -> "bb")
      }
      meta.entity.toString mustEqual expectedEntity.toString
    }
    "materialized schema meta for simple entity with trait as type alias" in {
      type SimpleEntityWithTrait1 = SimpleEntity with SimpleEntityTrait1
      val meta = materializeRefinedTypeSchemaMeta[SimpleEntityWithTrait1]
      val expectedEntity = quote {
        querySchema[SimpleEntityWithTrait1]("simple", _.a -> "aa", _.b -> "bb")
      }
      meta.entity.toString mustEqual expectedEntity.toString
    }
    "materialized schema meta for entity with more than 22 fields" in {
      val meta = materializeRefinedTypeSchemaMeta[TestEntityWith23Fields]
      val expectedEntity = quote {
        querySchema[TestEntityWith23Fields](
          "test_entity_with_more_than_22_fields",
          _.a1 -> "aa_1",
          _.a2 -> "aa_2",
          _.a3 -> "aa_3",
          _.a4 -> "aa_4",
          _.a5 -> "aa_5",
          _.a6 -> "aa_6",
          _.a7 -> "aa_7",
          _.a8 -> "aa_8",
          _.a9 -> "aa_9",
          _.a10 -> "aa_10",
          _.a11 -> "aa_11",
          _.a12 -> "aa_12",
          _.a13 -> "aa_13",
          _.a14 -> "aa_14",
          _.a15 -> "aa_15",
          _.a16 -> "aa_16",
          _.a17 -> "aa_17",
          _.a18 -> "aa_18",
          _.a19 -> "aa_19",
          _.a20 -> "aa_20",
          _.a21 -> "aa_21",
          _.a22 -> "aa_22",
          _.a23 -> "aa_23"
        )
      }
      meta.entity.toString mustEqual expectedEntity.toString
    }
    "materialized schema meta for entity with nested entities" in {
      val meta = materializeRefinedTypeSchemaMeta[TestEntityWithNested]
      val expectedEntity = quote {
        querySchema[TestEntityWithNested]("test_entity_custom_table_name", _.a -> "aaa", _.b.c -> "ccc", _.b.e -> "eee")
      }
      meta.entity.toString mustEqual expectedEntity.toString
    }
    "materialized schema meta for entity with trait with nested entities" in {
      val meta = materializeRefinedTypeSchemaMeta[SimpleEntity with TestTraitWithNested]
      val expectedEntity = quote {
        querySchema[SimpleEntity with TestTraitWithNested]("simple", _.a -> "aa", _.b -> "bb", _.c.c -> "ccc", _.c.e -> "eee")
      }
      meta.entity.toString mustEqual expectedEntity.toString
    }
    "materialized schema meta for entity with optionable nested" in {
      val meta = materializeRefinedTypeSchemaMeta[TestEntityWithOptionalNested]
      val expectedEntity = quote {
        querySchema[TestEntityWithOptionalNested]("test_entity", _.a -> "aaa", _.b.map(_.c) -> "ccc", _.b.map(_.e) -> "eee")
      }
      meta.entity.toString mustEqual expectedEntity.toString
    }
    "materialized schema meta for entity with trait with optionable nested" in {
      val meta = materializeRefinedTypeSchemaMeta[SimpleEntity with TestTraitWithOptionalNested]
      val expectedEntity = quote {
        querySchema[SimpleEntity with TestTraitWithOptionalNested]("simple", _.a -> "aa", _.b -> "bb", _.c.map(_.c) -> "ccc", _.c.map(_.e) -> "eee")
      }
      meta.entity.toString mustEqual expectedEntity.toString
    }
  }
}
