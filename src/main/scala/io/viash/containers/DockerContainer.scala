package io.viash.containers

import io.viash.schemas._

final case class DockerContainer(
  @description("Name of the container. As with all containers, you can give a container a different name. By specifying `id: foo`, you can target this container (only) by specifying `...` in any of the Viash commands.")
  @example("id: foo", "yaml")
  @default("docker")
  id: String = "docker",
) extends Container {

}
