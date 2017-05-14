package com.nikolastojiljkovic.quilltrait

import org.scalatest.{ BeforeAndAfterAll, FreeSpec, MustMatchers }

abstract class Spec extends FreeSpec with MustMatchers with BeforeAndAfterAll
