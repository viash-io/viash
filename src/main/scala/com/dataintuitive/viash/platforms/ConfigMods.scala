package com.dataintuitive.viash.platforms

import com.dataintuitive.viash.functionality.dataobjects.DataObject

case class ConfigMods(
  preParse: String = "",
  parsers: String = "",
  postParse: String = "",
  postRun: String = "",
  inputs: List[DataObject[_]] = Nil,
  extraParams: String = ""
) {
  def `++`(dm: ConfigMods): ConfigMods = {
    ConfigMods(
      preParse = preParse + dm.preParse,
      parsers = parsers + dm.parsers,
      postParse = postParse + dm.postParse,
      postRun = postRun + dm.postRun,
      inputs = inputs ::: dm.inputs,
      extraParams = extraParams + dm.extraParams
    )
  }
}