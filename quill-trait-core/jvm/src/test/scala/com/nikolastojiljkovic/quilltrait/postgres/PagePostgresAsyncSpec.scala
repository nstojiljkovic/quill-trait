package com.nikolastojiljkovic.quilltrait.postgres

import com.nikolastojiljkovic.quilltrait.PageSqlSpec
import com.nikolastojiljkovic.quilltrait.model.{ Page, SampleTraitWithNestable, SeoSupport }

import scala.concurrent.ExecutionContext.Implicits.{ global => ec }
import scala.concurrent.duration.Duration
import scala.concurrent.{ Await, Future }

class PagePostgresAsyncSpec extends PageSqlSpec {

  implicit val context = testContext

  import testContext._

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
      //      testContext.transaction { implicit ec =>
      //        for {
      //          _ <- testContext.run(deleteAllPages)
      //          _ <- testContext.run(liftQuery(testEntries).foreach(e => pageInsert(e)))
      //        } yield {}
      //      }

      for {
        _ <- testContext.run(deleteAllPages)
        _ <- Future.sequence(testEntries.map(p => testContext.run(pageInsert(lift(p)))))
      } yield {}
    }

  "Example 1 - sandbox" in {
    val result = await(testContext.run(`Ex 1`)).sortBy(_.getOrElse(-1))
    result mustEqual `Ex 1 expected result`
  }
  "Example 2 - sandbox" in {
    val result = await(testContext.run(`Ex 2`))
    result mustEqual `Ex 2 expected result`
    result.map(_.options1) mustEqual `Ex 2 expected result`.map(_.options1)
    // result.map(_.options2) mustEqual `Ex 2 expected result`.map(_.options2)
  }
}
