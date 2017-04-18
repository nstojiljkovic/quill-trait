package com.nikolastojiljkovic.quill

import io.getquill._

import scala.concurrent.{ Await, Future }
import scala.concurrent.duration.Duration

/**
 * Created by nikola on 4/19/17.
 */
package object cassandra {
  lazy val mirrorContext = new CassandraMirrorContext

  lazy val testSyncDB = new CassandraSyncContext[Literal]("testCassandraSyncDB") with AnnotatedTraitSupport

  lazy val testAsyncDB = new CassandraAsyncContext[Literal]("testCassandraAsyncDB") with AnnotatedTraitSupport

  lazy val testStreamDB = new CassandraStreamContext[Literal]("testCassandraStreamDB") with AnnotatedTraitSupport

  def await[T](f: Future[T]): T = Await.result(f, Duration.Inf)
}
