package com.nikolastojiljkovic.quilltrait

import io.getquill.{ MysqlAsyncContext, MysqlEscape }

package object mysql {
  object testContext extends MysqlAsyncContext[MysqlEscape]("testMysqlDB") with AnnotatedTraitSupport
}
