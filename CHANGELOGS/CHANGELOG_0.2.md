# Viash 0.2.2 (2020-09-22): Generation of placeholder code now possible without VIASH START and VIASH END

Allow generating placeholder without VIASH START/VIASH END blocks.

A script does not need to contain a `VIASH START`/`VIASH END` block in order to function.

Previously, each script had to contain a codeblock as follows:

  ```r
  ## VIASH START
  par <- list(
    input = "foo",
    output = "bar
  )
  ## VIASH END
  ```

## MINOR CHANGES

* Allow generating placeholder without VIASH START/VIASH END blocks.

## BUG FIXES

* `viash ns build`: Some platforms would sometimes not be detected.
* `viash run`: Avoid error when no arguments need to be chowned.

# Viash 0.2.1 (2020-09-11): Docker chown by default

## Docker chown by default

Running a script using a Docker platform will now chown output files by default, as well as any temporary files. You can turn off this feature by specifying `chown: false` in the yaml of a Docker platform.

## [NXF] Data references

Data references in Map form can now have values being lists. In other words, we can have multiple options which have one or more values.

## viash ns build -P docker --parallel --setup

`viash ns build` has been greatly improved! You can automatically build the docker container by adding `--setup` to the command, as well as make the whole thing run in parallel using the `--parallel` or `-l` flag.

To build a docker container, you can run either of the following:

  ```bash
  viash run -f path/to/config.yaml -P docker -- ---setup
  viash build -f path/to/functionality.yaml -P docker -o target/docker/path/to --setup
  ```

Note that the first will only build the docker container, whereas the second will build the executable and then build the docker container.

To build a lot of them all at once, run:

  ```bash
  viash ns build -P docker --parallel --setup
  ```

## Custom order of platform requirements

You can now choose the order in which platform requirements are installed!

Before:

  ```yaml
  type: docker
  image: rocker/tidyverse
  target_image: "viash_test/r"
  r:
    cran:
    - optparse
    github:
    - dynverse/dynutils@devel
    bioc:
    - limma
  apt:
    packages:
    - libhdf5-serial-dev
  docker:
    build_arg:
    - GITHUB_PAT="$GITHUB_PAT"
    run:
    - git clone --depth 1 https://github.com/data-intuitive/viash_docs.git && rm -r viash_docs/.git
  ↑ in which order will these three components be run? Who knows!
  ```

Now:

  ```yaml
  type: docker
  image: rocker/tidyverse
  target_image: "viash_test/r"
  setup:
  - type: docker
    build_arg:
    - GITHUB_PAT="$GITHUB_PAT"
  - type: apt
    packages:
    - libhdf5-serial-dev
  - type: r
    cran:
    - optparse
    - dynutils
    github:
    - rcannood/princurve@devel
    bioc:
    - limma
  - type: docker
    run:
    - git clone --depth 1 https://github.com/data-intuitive/viash_docs.git && rm -r viash_docs/.git
  ```

This will ensure that the setup instructions are installed in the given order.

## NEW FEATURES

* `NXF`: Data references in Map form can now have values being lists. In other words, we can have multiple options which have one or more values.
* `viash ns build`: Added --parallel and --setup flag.
* `viash build`: Added --setup flag.
* Allow changing the order of setup commands using the `setup:` variable.
* (HIDDEN) Do not escape `${VIASH_...}` elements in default values and descriptions!

## MINOR CHANGES

* Remove `---chown` flag, move to `platform.docker.chown`; is set to true by default.
* Perform chown during both run and test using a Docker platform.

## BUG FIXES

* Issue trying to parse positional arguments even when none is provided.

# Viash 0.2.0 (2020-09-01): Autoresolve docker paths

## Changes to functionality metadata

- Added version attribute

### Autoresolve docker paths

Arguments of type: file are processed to automatically create a mount in docker. More specifically, when you pass an argument value: `--input /path/to/file`, this will be processed such that the following parameters are passed to docker:

  ```bash
  docker run -v /path/to:/viash_automount/path/to ... --input /viash_automount/path/to/file
  ```

If, for some reason, you need to manually specify a mount, you can do this with `---mount /path/to/mount:/mymount`.

### Argument multiplicity

For all parameter types (except for `boolean_true` and `boolean_false`), you can specify `multiple: true` in order to turn this argument into an array-based argument. What this does is allow you to pass multiple values for this argument, e.g. `--input file1 --input file2 --input file3:file4:file5`.

The default separator is `:` but this can be overridden by changing the separator by setting it to `multiple_sep: ";"` (for example).

### New format

Viash now supports placing the functionality.yaml, platform*.yaml(s) and script into a single file. For example, this could be a merged script.R:

  ```r
  #' functionality:
  #'   name: r-estimate
  #'   arguments: ...
  #' platforms:
  #' - type: native
  #' - type: docker
  #'   image: rocker/tidyverse
  library(tidyverse)
  cat("Hello world!\n")
  ```

Instead of running:

  ```bash
  viash run -f functionality.yaml -p platform_docker.yaml -- arg1
  ```

With this format, you can now run:

  ```bash
  viash run script.R                     # run script.R with the first platform
  viash run -P docker script.R           # run script.R with the platform called 'docker' with the large P argument
  # use small p to override the platform with a custom yaml:
  viash run -p common_resources/platform_docker.yaml script.R
  # note that any arguments for the run command (e.g. -p or -P) should come before the script.R, as script.R is considered a trailing argument.
  ```

## NEW FEATURES

* Allow (optional) version attributes in `functionality.yaml` and `platform.yaml`.
* Allow testing a component with the `viash test` functionality. Tests are executed in a temporary directory on the specified platform. The temporary directory contains all the resource and test files. 
* `viash --version`: Add flag for printing the version of viash.
* Allow fetching resources from URL (http:// and https://)
* Allow retrieving functionality and platform YAMLs from URL.
* For docker containers, autoresolve path names of files. Use `---v path:path` or `---volume path:path` to manually mount a specific folder.
* Implement parameter multiplicity. 
  Set `multiple: true` to denote an argument to have higher multiplicity. 
  Run `./cmd --foo one --foo two --foo three:four` in order for multiple values to be added to the same parameter list.
* Added a new format for defining functionality in which the user passes the script in which the functionality and platforms are listed as yaml headers.
* A `---chown` flag has been added to Docker executables to automatically change the ownership of output files to the current user.
* `viash ns build`: A command for building a whole namespace.
* `NXF`: Join operations are now fully supported by means of `multiple`.
* `NXF`: Modules that perform joins can take either arrays (multiple input files or the same type to be joined) or hashes (multiple input files passed using different options on the CLI). Please refer to the docs for more info.

## MAJOR CHANGES

* Remove passthrough parameters.
* Since CLI generation is now performed in the outer script, `viash pimp` has been deprecated.
* Write out meta.yaml containing viash run information as well as the original `functionality.yaml` and `platform.yaml` content.
* Renamed `viash export` to `viash build`.

## MINOR CHANGES

* `viash run` and `viash test`: Allow changing the temporary directory by defining `VIASH_TEMP` as a environment variable. Temporary directories are cleaned up after successful executions.
* `viash run` and `viash test`: Exit(1) when execution or test fails.
* `viash build`: Add -m flag for outputting metadata after build.
* `viash run`: Required parameters can have a default value now. Produce error when a required parameter is not passed, even when a default is provided.
* `NXF`: _Modules_ are now stored under `target/nextflow` by default

## BUG FIXES

* `NXF`: Correctly escape path variable when running NXF command.
* `NXF`: Surround parameters with quotes when running NXF command.

## INTERNAL CHANGES

* Move CLI from inner script to outer script.
* Renamed Target to Platform
* Renamed Environment to Requirements
