package com.dataintuitive.viash.functionality

sealed trait Direction
case object Input extends Direction
case object Output extends Direction
case object Log extends Direction