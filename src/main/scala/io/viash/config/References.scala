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

package io.viash.config

import io.viash.schemas._
import io.viash.helpers.data_structures.{OneOrMore, listToOneOrMore}

@description("A list of scholarly sources or publications relevant to the tools or analysis defined in the component. This is important for attribution, scientific reproducibility and transparency.")
@example(
  """references:
    |  doi: 10.1000/xx.123456.789
    |  bibtex: |
    |    @article{foo,
    |      title={Foo},
    |      author={Bar},
    |      journal={Baz},
    |      year={2024}
    |    }
    |""", "yaml")
@since("Viash 0.9.0")
case class References(
  @description("One or multiple DOI reference(s) of the component.")
  @example("doi: 10.1000/xx.123456.789", "yaml")
  @default("Empty")
  doi: OneOrMore[String] = Nil,

  @description("One or multiple BibTeX reference(s) of the component.")
  @example(
    """bibtex: |
      |  @article{foo,
      |    title={Foo},
      |    author={Bar},
      |    journal={Baz},
      |    year={2024}
      |  }
      |""", "yaml")
  @default("Empty")
  bibtex: OneOrMore[String] = Nil,
)
