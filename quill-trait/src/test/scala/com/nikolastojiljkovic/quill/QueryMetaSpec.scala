package com.nikolastojiljkovic.quill

import com.nikolastojiljkovic.annotation.{ Embedded, Field, Table }
import io.getquill.context.mirror.Row
import io.getquill.{ MirrorContext, MirrorSqlDialect, MysqlEscape }

class QueryMetaSpec extends Spec {

  @Table("simple")
  case class SimpleEntity(
    @Field("aa") a: String
  )

  @Table("test_entity_with_more_than_22_fields")
  case class TestEntityWith23Fields(
    @Field("aa_1") a1:   Int,
    @Field("aa_2") a2:   Int,
    @Field("aa_3") a3:   Int,
    @Field("aa_4") a4:   Int,
    @Field("aa_5") a5:   Int,
    @Field("aa_6") a6:   Int,
    @Field("aa_7") a7:   Int,
    @Field("aa_8") a8:   Int,
    @Field("aa_9") a9:   Int,
    @Field("aa_10") a10: Int,
    @Field("aa_11") a11: Int,
    @Field("aa_12") a12: Int,
    @Field("aa_13") a13: Int,
    @Field("aa_14") a14: Int,
    @Field("aa_15") a15: Int,
    @Field("aa_16") a16: Int,
    @Field("aa_17") a17: Int,
    @Field("aa_18") a18: Int,
    @Field("aa_19") a19: Int,
    @Field("aa_20") a20: Int,
    @Field("aa_21") a21: Int,
    @Field("aa_22") a22: Int,
    @Field("aa_23") a23: Int
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

  trait SimpleEntityTrait2 extends SimpleEntityCopy {
    this: SimpleEntity =>

    @Field("bb")
    def b: String

    @Field("cc")
    def c: Option[String]
  }

  implicit object context extends MirrorContext[MirrorSqlDialect, MysqlEscape] with AnnotatedTraitSupport

  import context._

  "query meta" - {
    "materialized query meta for simple entity" in {
      val meta = materializeRefinedTypeQueryMeta[SimpleEntity]
      val expectedExpand = quote {
        (q: Query[SimpleEntity]) => q.map(x => x.a)
      }
      meta.expand.toString mustEqual expectedExpand.toString
      meta.extract(Row("1")) mustEqual SimpleEntity("1")
    }
    "materialized query meta for simple entity with trait" in {
      type SimpleEntityWithTrait1 = SimpleEntity with SimpleEntityTrait1
      val meta = materializeRefinedTypeQueryMeta[SimpleEntity with SimpleEntityTrait1]
      val expectedExpand = quote {
        (q: Query[SimpleEntity with SimpleEntityTrait1]) =>
          q.map(x => io.getquill.dsl.UnlimitedTuple(
            x.a,
            x.b
          ))
      }
      meta.expand.toString mustEqual expectedExpand.toString

      meta.extract(Row("1", "2")) mustEqual new SimpleEntity("1") with SimpleEntityTrait1 {
        val b = "2"
      }
    }
    "materialized query meta for simple entity with trait as type alias" in {
      type SimpleEntityWithTrait1 = SimpleEntity with SimpleEntityTrait1
      val meta = materializeRefinedTypeQueryMeta[SimpleEntityWithTrait1]
      val expectedExpand = quote {
        (q: Query[SimpleEntityWithTrait1]) =>
          q.map(x => io.getquill.dsl.UnlimitedTuple(
            x.a,
            x.b
          ))
      }
      meta.expand.toString mustEqual expectedExpand.toString

      meta.extract(Row("1", "2")) mustEqual new SimpleEntity("1") with SimpleEntityTrait1 {
        val b = "2"
      }
    }
    "materialized query meta for simple entity with multiple traits" in {
      type SimpleEntityWithTrait1And2 = SimpleEntity with SimpleEntityTrait1 with SimpleEntityTrait2
      val meta = materializeRefinedTypeQueryMeta[SimpleEntity with SimpleEntityTrait1 with SimpleEntityTrait2]
      val expectedExpand = quote {
        (q: Query[SimpleEntity with SimpleEntityTrait1 with SimpleEntityTrait2]) =>
          q.map(x => io.getquill.dsl.UnlimitedTuple(
            x.a,
            x.b,
            x.c
          ))
      }
      meta.expand.toString mustEqual expectedExpand.toString
      meta.extract(Row("1", "2", Some("3"))) mustEqual new SimpleEntity("1") with SimpleEntityTrait1 with SimpleEntityTrait2 {
        val b = "2"
        val c = Some("3")
      }
    }
    "materialized query meta for entity with more than 22 fields" in {
      val meta = materializeRefinedTypeQueryMeta[TestEntityWith23Fields]
      val expectedExpand = quote {
        (q: Query[TestEntityWith23Fields]) =>
          q.map(x => io.getquill.dsl.UnlimitedTuple(
            x.a1,
            x.a2,
            x.a3,
            x.a4,
            x.a5,
            x.a6,
            x.a7,
            x.a8,
            x.a9,
            x.a10,
            x.a11,
            x.a12,
            x.a13,
            x.a14,
            x.a15,
            x.a16,
            x.a17,
            x.a18,
            x.a19,
            x.a20,
            x.a21,
            x.a22,
            x.a23
          ))
      }
      meta.expand.toString mustEqual expectedExpand.toString
      meta.extract(Row(0 until 30: _*)) mustEqual
        TestEntityWith23Fields(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22)
    }
    "materialized query meta for entity with nested entities" in {
      val meta = materializeRefinedTypeQueryMeta[TestEntityWithNested]
      val expectedExpand = quote {
        (q: Query[TestEntityWithNested]) =>
          q.map(x => io.getquill.dsl.UnlimitedTuple(
            x.a,
            x.b.c,
            x.b.e
          ))
      }
      meta.expand.toString mustEqual expectedExpand.toString
      meta.extract(Row("1", "2", Some("3"))) mustEqual TestEntityWithNested("1", NestedEntity("2", Some("3")))
    }
    "materialized query meta for entity with optionable nested" - {
      "extracts Some if all columns are defined" in {
        val meta = materializeRefinedTypeQueryMeta[TestEntityWithOptionalNested]
        val expectedExpand = quote {
          (q: Query[TestEntityWithOptionalNested]) =>
            q.map(x => io.getquill.dsl.UnlimitedTuple(
              x.a,
              x.b.map(v => v.c),
              x.b.map(v => v.e)
            ))
        }
        meta.expand.toString mustEqual expectedExpand.toString
        meta.extract(Row("1", Some("2"), Some("3"))) mustEqual TestEntityWithOptionalNested("1", Some(NestedEntity("2", Some("3"))))
      }
      "extracts Some if one non-required column is undefined" in {
        val meta = materializeRefinedTypeQueryMeta[TestEntityWithOptionalNested]
        val expectedExpand = quote {
          (q: Query[TestEntityWithOptionalNested]) =>
            q.map(x => io.getquill.dsl.UnlimitedTuple(
              x.a,
              x.b.map(v => v.c),
              x.b.map(v => v.e)
            ))
        }
        meta.expand.toString mustEqual expectedExpand.toString
        meta.extract(Row("1", Some("2"), None)) mustEqual TestEntityWithOptionalNested("1", Some(NestedEntity("2", None)))
      }
      "extracts None if one column is undefined" in {
        val meta = materializeRefinedTypeQueryMeta[TestEntityWithOptionalNested]
        val expectedExpand = quote {
          (q: Query[TestEntityWithOptionalNested]) =>
            q.map(x => io.getquill.dsl.UnlimitedTuple(
              x.a,
              x.b.map(v => v.c),
              x.b.map(v => v.e)
            ))
        }
        meta.expand.toString mustEqual expectedExpand.toString
        meta.extract(Row("1", None, Some("3"))) mustEqual TestEntityWithOptionalNested("1", None)
      }
    }
  }
}
