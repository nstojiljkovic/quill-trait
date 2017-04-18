package com.nikolastojiljkovic.quill

import io.getquill.{ PostgresAsyncContext, PostgresEscape }

package object postgres {
  object testContext extends PostgresAsyncContext[PostgresEscape]("testPostgresDB") with AnnotatedTraitSupport
}
