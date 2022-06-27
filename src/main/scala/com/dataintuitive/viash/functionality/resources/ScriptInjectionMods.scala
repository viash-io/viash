package com.dataintuitive.viash.functionality.resources

case class ScriptInjectionMods(
  header: String = "",
  params: String = "",
  footer: String = ""
) {
  def `++`(other: ScriptInjectionMods): ScriptInjectionMods = {
    ScriptInjectionMods(
      header = header + other.header,
      params = params + other.params,
      footer = footer + other.footer
    )
  }
}
