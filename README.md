# Viash: A meta-framework for building reusable workflow modules


<figure>
<a href="https://viash.io"><img
src="https://viash.io/logo/viash_large.svg" alt="Viash" /></a>
<figcaption>Viash</figcaption>
</figure>

[![https://img.shields.io/github/license/viash-io/viash?style=flat-square](License.png)](https://github.com/viash-io/viash/blob/master/LICENSE.md)
[![GitHub
Release](https://img.shields.io/github/v/release/viash-io/viash?style=flat-square.png)](https://github.com/viash-io/viash/releases)
[![Scala
CI](https://github.com/viash-io/viash/actions/workflows/sbt_test.yml/badge.svg)](https://github.com/viash-io/viash/actions/workflows/sbt_test.yml)
[![DOI](https://joss.theoj.org/papers/10.21105/joss.06089/status.svg)](https://doi.org/10.21105/joss.06089)

Viash helps you turn a script (Bash/R/Python/Scala/JavaScript) into a
reusable component. By providing some meta-data regarding its
functionality and the platform on which you want to run the software,
Viash can help you:

- Wrap your script in an executable with a CLI and –help functionality,
- Seamlessly execute your component natively on the host platform or in
  a Docker container
- Combine multiple components in a Nextflow pipeline, and
- Unit-test your component to ensure that it works at all times.

## Documentation

The Viash documentation is available online at
[`viash.io`](https://viash.io).

## License

Copyright (C) 2020 Data Intuitive

This program is free software: you can redistribute it and/or modify it
under the terms of the GNU General Public License as published by the
Free Software Foundation, either version 3 of the License, or (at your
option) any later version.

This program is distributed in the hope that it will be useful, but
WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
Public License for more details.

You should have received a copy of the GNU General Public License along
with this program. If not, see <http://www.gnu.org/licenses/>.
