package com.nikolastojiljkovic.quilltrait.cassandra

import com.nikolastojiljkovic.quilltrait.PageSpec
import com.nikolastojiljkovic.quilltrait.model._

import scala.concurrent.{ Await, Future }
import scala.concurrent.duration.Duration
import scala.concurrent.ExecutionContext.Implicits.global

class PageCassandraAsyncSpec extends PageSpec {

  implicit val context = testAsyncDB

  import testAsyncDB._

  def await[T](f: Future[T]) = Await.result(f, Duration.Inf)

  val pages = quote {
    query[Page with SeoSupport with SampleTraitWithNestable]
  }

  def deleteAllPages = quote {
    pages.delete
  }

  val pageInsert = quote {
    (p: Page with SeoSupport with SampleTraitWithNestable) => pages.insert(p)
  }

  override def beforeAll =
    await {
      for {
        _ <- testAsyncDB.run(deleteAllPages)
        _ <- Future.sequence(testEntries.map(p => testAsyncDB.run(pageInsert(lift(p)))))
      } yield {}
    }

  val ids = List(Some(1), Some(2), Some(3), Some(4))
  val `Ex 1` =
    quote {
      for {
        c <- query[Page with SeoSupport with SampleTraitWithNestable]
      } yield c
    }
  val `Ex 1 expected result` = List(("Root", "monthly"), ("Home page", "hourly"), ("Child page 1", "weekly"), ("Child page 2", "weekly"))

  "Example 1 - sandbox" in {
    await(testAsyncDB.run(`Ex 1`)).sortBy(_.id.getOrElse(0)).map(t => (t.title, t.sitemapChangeFrequency)) mustEqual `Ex 1 expected result`
  }
}
