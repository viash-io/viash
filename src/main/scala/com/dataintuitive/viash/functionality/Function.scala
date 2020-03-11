package com.dataintuitive.viash.functionality

case class Functionality(
  name: String,
  description: String,
  command: String, 
  parameters: Seq[Parameter[_]], 
  resources: Seq[Resource]
)