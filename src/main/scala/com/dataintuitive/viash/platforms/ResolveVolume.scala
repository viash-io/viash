package com.dataintuitive.viash.platforms

sealed trait ResolveVolume
case object Manual extends ResolveVolume
case object Automatic extends ResolveVolume
