package com.nikolastojiljkovic.quill

import io.getquill.{ MysqlAsyncContext, MysqlEscape }

package object mysql {
  object testContext extends MysqlAsyncContext[MysqlEscape]("testMysqlDB") with AnnotatedTraitSupport
}
