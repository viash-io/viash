package com.dataintuitive.viash.platforms.docker

sealed trait DockerResolveVolume

case object Manual extends DockerResolveVolume

case object Automatic extends DockerResolveVolume
