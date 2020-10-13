package com.dataintuitive.viash.functionality

sealed trait FunctionType

case object AsIs extends FunctionType

case object Convert extends FunctionType

case object ToDir extends FunctionType

case object Join extends FunctionType