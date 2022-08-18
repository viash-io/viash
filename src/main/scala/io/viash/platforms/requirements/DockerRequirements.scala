/*
 * Copyright (C) 2020  Data Intuitive
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package io.viash.platforms.requirements

import io.viash.helpers.Circe._
import io.viash.helpers.description
import io.viash.helpers.example

@description("Specify which Docker commands should be run during setup.")
@example("""setup:
           |  - type: docker
           |    build_args: [ GITHUB_PAT=hello_world ]
           |    run: [ git clone ... ]
           |    add: [ "http://foo.bar ." ]
           |    copy: [ "http://foo.bar ." ]
           |    resources: 
           |      - resource.txt /path/to/resource.txt
           |""".stripMargin, "yaml")
case class DockerRequirements(
  resources: OneOrMore[String] = Nil,
  label: OneOrMore[String] = Nil,
  add: OneOrMore[String] = Nil,
  copy: OneOrMore[String] = Nil,
  run: OneOrMore[String] = Nil,
  build_args: OneOrMore[String] = Nil,
  env: OneOrMore[String] = Nil,
  `type`: String = "docker"
) extends Requirements {
  def installCommands: List[String] = Nil

  override def dockerCommands: Option[String] = {
    val args =
      if (build_args.nonEmpty) {
        build_args.map(s => "ARG " + s.takeWhile(_ != '='))
      } else {
        Nil
      }

    val labels =
      if (label.nonEmpty) {
        label.map(c => s"""LABEL $c""")
      } else {
        Nil
      }

    val copys =
      if (copy.nonEmpty) {
        copy.map(c => s"""COPY $c""")
      } else {
        Nil
      }

    val adds =
      if (add.nonEmpty) {
        add.map(c => s"""ADD $c""")
      } else {
        Nil
      }

    val resourcess =
      if (resources.nonEmpty) {
        resources.map(c => s"""COPY $c""")
      } else {
        Nil
      }

    val envs =
      if (env.nonEmpty) {
        env.map(c => s"""ENV $c""")
      } else {
        Nil
      }

    val runCommands =
      if (run.nonEmpty) {
        run.map(r => s"""RUN $r""")
      } else {
        Nil
      }

    val li = args ::: labels ::: envs ::: copys ::: resourcess ::: adds ::: runCommands

    if (li.isEmpty) None else Some(li.mkString("\n"))
  }


}
