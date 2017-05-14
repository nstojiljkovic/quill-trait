package com.nikolastojiljkovic.quilltrait

import io.getquill.{ PostgresAsyncContext, PostgresEscape }

package object postgres {
  object testContext extends PostgresAsyncContext[PostgresEscape]("testPostgresDB") with AnnotatedTraitSupport
}
