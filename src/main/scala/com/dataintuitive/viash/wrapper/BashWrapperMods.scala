package com.dataintuitive.viash.wrapper

import com.dataintuitive.viash.functionality.dataobjects.DataObject

case class BashWrapperMods(
   preParse: String = "",
   parsers: String = "",
   postParse: String = "",
   postRun: String = "",
   inputs: List[DataObject[_]] = Nil,
   extraParams: String = ""
 ) {
  def `++`(other: BashWrapperMods): BashWrapperMods = {
    BashWrapperMods(
      preParse = preParse + other.preParse,
      parsers = parsers + other.parsers,
      postParse = postParse + other.postParse,
      postRun = postRun + other.postRun,
      inputs = inputs ::: other.inputs,
      extraParams = extraParams + other.extraParams
    )
  }
}
