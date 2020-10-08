package com.dataintuitive.viash.helpers

object Scala {
  implicit class AugmentedOption[T](opt: Option[T]) {
    def `|`(other: Option[T]): Option[T] = {
      if (opt.isDefined) {
        opt
      } else {
        other
      }
    }
  }
}
