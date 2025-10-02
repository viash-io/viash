# Viash 0.6.7 (2022-12-14): A minor release with several QoL improvements

Another minor release which contains several quality of life improvements for the Nextflow VDSL3 platform, as well as automated warnings for deprecated functionality.

## MINOR CHANGES

* `NextflowPlatform`: Create directories during a stub run when output path is a nested directory (PR #314).

* Automatically generate a warning for deprecated parameters while parsing a .viash.yaml configuration file using the inline documentation deprecation annotations.

* Add a "planned removal" field in the deprecation annotations.

* Add testbenches to verify proper formatting of the deprecation versions and compare current version to the planned removal version so no deprecated parameters get to stick around beyond what was planned.

* `NextflowPlatform`: Nextflow processes are created lazily; that is, only when running
  a Nextflow workflow (PR #321).

## BUG FIXES

* `NextflowPlatform`: Automatically split Viash config strings into strings of 
  length 65000 since the JVM has a limit (65536) on the length of string constants (PR #323).

# Viash 0.6.6 (2022-12-06): A small bugfix release

This release fixes an issue where stderr was being redirected to stdout.

## BUG FIXES

* Don't redirect stderr to stdout when switching Viash versions (#312).

# Viash 0.6.5 (2022-12-02): A small bugfix release

A small update which fixes an issue with `viash ns list` that was
introduced in Viash 0.6.3.

## BUG FIXES

* `viash ns list`: When the `-p <platform>` is defined, filter the 
  output by that platform.

# Viash 0.6.4 (2022-11-30): Add backwards compability by supporting switching to older Viash versions

This release adds features related to managing Viash projects and 
allows for better runtime introspection of Nextflow VDSL3 modules.

The most notable changes are:

* You can switch versions of Viash using the `VIASH_VERSION` 
  environment variable! Example:
  
  ```bash
  VIASH_VERSION=0.6.0 viash --version
  ```

  More importantly, you can specify the version of Viash you want
  in a project config. See below for more info.

* Introducing Viash project config files as an experimental feature.
  It allows storing project-related settings in a `_viash.yaml` 
  config file which you should store at the root of your repository.
  Example:

  ```yaml
  viash_version: 0.6.4
  source: src
  target: target
  config_mods: |
    .platforms[.type == 'docker'].target_registry := 'ghcr.io'
    .platforms[.type == 'docker'].target_organization := 'viash-io'
    .platforms[.type == 'docker'].namespace_separator := '/'
    .platforms[.type == 'docker'].target_image_source := 'https://github.com/viash-io/viash'
  ```

* It's now possible to specify in which order Viash will merge
  Viash configs. Example:

  ```yaml
  functionality:
    name: foo
    arguments:
      - __merge__: obj_input.yaml
        name: "--one"
      - __merge__: [., obj_input.yaml]
        name: "--two"
      - __merge__: [obj_input.yaml, .]
       name: "--three"
  ```

Please take note of the following breaking changes:

* Passing non-existent paths to a Viash component will cause the 
  component to generate an error when no file or folder is found.
  Set `must_exist` to `false` to revert to the previous behaviour.

* The arguments `--write_meta/-w` and `--meta/-m` no longer exist,
  because every `viash build/run/test` run will generate a 
  `.config.vsh.yaml` meta file.

## BREAKING CHANGES

* Config: Viash configs whose filenames start with a `.` are ignored (PR #291).

* `viash build`: `--write_meta/-m` and `--meta/-m` arguments have been removed. 
  Instead, the `.config.vsh.yaml` file is always created when building Viash components (PR #293).

* `FileArgument`: Default setting of `must_exist` was changed from `false` to `true`. 
  As such, the component will throw an error by default if an input file or output file
  is missing (PR #295).

* Config merging: `__inherits__` has been renamed to `__merge__`.

## NEW FUNCTIONALITY

* You can switch versions of Viash using the `VIASH_VERSION` 
  environment variable (PR #304)! Example:
  
  ```bash
  VIASH_VERSION=0.6.0 viash --version
  ```

* Traceability: Running `viash build` and `viash test` creates a `.config.vsh.yaml` file 
  by default, which contains the processed config of the component. As a side effect, 
  this allows for reading in the `.config.vsh.yaml` from within the component to learn 
  more about the component being tested (PR #291 and PR #293).

* `FileArgument`: Added `create_parent` option, which will check if the directory of an output
file exists and create it if necessary (PR #295).

## MINOR CHANGES

* `viash run`, `viash test`: When running or testing a component, Viash will add an extension
  to the temporary file that is created. Before: `/tmp/viash-run-wdckjnce`, 
  now: `/tmp/viash-run-wdckjnce.py` (PR #302).

* NextflowPlatform: Add `DataflowHelper.nf` as a retrievable resource in Viash (PR #301).

* NextflowPlatform: During a stubrun, argument requirements are turned off and
  the `publishDir`, `cpus`, `memory`, and `label` directives are also removed 
  from the process (PR #301).

* `NextflowPlatform`: Added a `filter` processing argument to filter the incoming channel after 
  the `map`, `mapData`, `mapId` and `mapPassthrough` have been applied (PR #296).

* `NextflowPlatform`: Added the Viash config to the Nextflow module for later introspection (PR #296).
  For example:
  ```groovy
  include { foo } from "$targetDir/path/foo/main.nf"

  foo.run(filter: { tup ->
    def preferredNormalization = foo.config.functionality.info.preferred_normalization
    tup.normalization_id == preferredNormalization
  })
  ```
## BUG FIXES

* `BashWrapper`: Don't overwrite meta values when trailing arguments are provided (PR #295).

## EXPERIMENTAL FEATURES

* Viash Project: Viash will automatically search for a `_viash.yaml` file in the directory of 
  a component and its parent directories (PR #294).

  Contents of `_viash.yaml`:
  ```yaml
  source: src
  target: target
  config_mods: |
    .platforms[.type == 'docker'].target_registry := 'ghcr.io'
    .platforms[.type == 'docker'].target_organization := 'viash-io'
    .platforms[.type == 'docker'].namespace_separator := '/'
    .platforms[.type == 'docker'].target_image_source := 'https://github.com/viash-io/viash'
  ```

* Config merging: Allow specifying the order in which Viash will merge configs (PR #289).
  If `.` is not in the list of inherited objects, it will be added at the end.

  Contents of `config.vsh.yaml`:
  ```yaml
  functionality:
    name: foo
    arguments:
      - __merge__: obj_input.yaml
        name: "--one"
      - __merge__: [., obj_input.yaml]
        name: "--two"
      - __merge__: [obj_input.yaml, .]
        name: "--three"
  ```

  Contents of `obj_input.yaml`:
  ```yaml
  type: file
  name: --input
  description: A h5ad file
  ```
  Output of `viash config view config.vsh.yaml` (stripped irrelevant bits):
  ```yaml
  functionality:
    arguments:
    - type: "file"
      name: "--one"
      description: "A h5ad file"
    - type: "file"
      name: "--input"
      description: "A h5ad file"
    - type: "file"
      name: "--three"
      description: "A h5ad file"
  ```
  

# Viash 0.6.3 (2022-11-09): Quality-of-life improvements in Viash.

This release features contains mostly quality of life improvements and some experimental functionality. Most notably:

* `viash ns list` now only returns a config just once instead of once per platform.

* A functionality's info field can contain any data structures. An `.info` field was added to arguments as well.

* Bug fixes for using Viash with podman, Nextflow>=22.10 and R<4.0.

* Experimental support for inheriting from config partials.

## MAJOR CHANGES

* `Config`: Made major internal changes w.r.t. how config files are read and at which point a platform (native, docker, nextflow)
  is applied to the functionality script. The only visible side effect is that 
  `viash ns list` will output each config only once instead of multiple times.

* `Functionality`: Structured annotation can be added to a functionality and its arguments using the `info` field. Example:
  ```yaml
  functionality:
    name: foo
    info:
      site: https://abc.xyz
      tags: [ one, two, three ]
    arguments:
      - name: --foo
        type: string
        info:
          foo: bar
          a:
            b:
              c
  ```

## MINOR CHANGES

* `BashWrapper`: Allow printing the executor command by adding `---verbose ---verbose` to a `viash run` or an executable.

* `Testbenches`: Rework `MainBuildAuxiliaryNativeParameterCheck` to create stimulus files and loop over the file from bash instead of looping natively.
  This prevents creating thousands of new processes which would only test a single parameter.
  Note this still calls the main script for each stimulus separately, but that was the case anyway, only much much worse.

* `Testbenches`: Split some grouped test benches into slightly smaller test benches that group tested functionality better.

* `Annotations`: Complete the config schema annotations.
  Make sure all arguments are documented.
  Added an annotation `internalFunctionality` and `undocumented` for arguments that should not be documented.
  Added a testbench that verifies that all arguments are in fact annotated, skipping those that are not in the class constructor.
  Adds a hierarchy field in the `__this__` member to list the relation of the own and parent classes.

* `Testbenches`: Add exit code to helper method `testMainWithStdErr`.

* `Testbenches`: Add testbench to verify viash underscore components (viash_build, viash_install, viash_push, viash_skeleton, viash_test).

* `Testbenches`: Update viash underscore component tests to use `$meta_executable`.

* `viash ns exec`: Allow choosing whether the `{platform}` field should be filled in, based on the `--apply_platform` parameter.

## BUG FIXES

* `DockerPlatform`: Remove duplicate auto-mounts (#257).

* `Underscore component tests`: Fix tests for `viash_skeleton` and `viash_test` components.

* `NextflowVDSL3Platform`: Fix 'Module scriptPath has not been defined yet' error when Nextflow>=22.10 (#269).

* `config inject`: Doesn't work when `must_exist == true` (#273).

* `RScript`: Fix compatibility issue where the new character escaping in `r_script` required R>=4.0 (#275). Escaping is now handled without
  using the new `r'(foo)'` notation.

## DEPRECATION

* `DockerRequirements`: The `resources:` setting has been deprecated and will be removed in Viash 0.7.0. Please use `copy:` instead.

* `DockerRequirements`: The `privileged:` setting has been deprecated and will be removed in Viash 0.7.0. Please use `run_args: "--privileged"` instead.

## EXPERIMENTAL FUNCTIONALITY

* `Config`: Any part of a Viash config can use inheritance to fill data (PR #271). For example:
  Contents of `src/test/config.vsh.yaml`:
  ```yaml
  __inherits__: ../api/base.yaml
  functionality:
    name: test
    resources:
      - type: bash_script
        path: script.sh
        text: |
          echo Copying $par_input to $par_output
          cp $par_input $par_output
  ```
  Contents of `src/api/base.yaml`:
  ```yaml
  functionality:
    arguments:
      - name: "--input"
        type: file
      - name: "--output"
        type: file
        direction: output
  ```
  The resulting yaml will be:
  ```yaml
  functionality:
    name: test
    arguments:
      - name: "--input"
        type: file
      - name: "--output"
        type: file
        direction: output
    resources:
      - type: bash_script
        path: script.sh
        text: |
          echo Copying $par_input to $par_output
          cp $par_input $par_output
  ```

# Viash 0.6.2 (2022-10-11): Two bug fixes

This is a quick release to push two bug fixes related to security and being able to run Nextflow with optional output files.

## BUG FIXES

* `Git`: Strip credentials from remote repositories when retrieving the path.

* `VDSL3`: Allow optional output files to be `null`.

# Viash 0.6.1 (2022-10-03): Minor improvements in functionality

This release contains mostly minor improvements of functionality released in Viash 0.6.0. Most notably:

* Support was added for `type: long` arguments

* `meta["n_proc"]` has been renamed to `meta["cpus"]`. `meta["cpus"]` is now an integer, whereas `meta["memory_*"]` are now longs.

* `viash ns exec` is able to recognise `{platform}` and `{namespace}` fields.

* And various bug fixes and improvements to documentation and unit testing.

## BREAKING CHANGES

* Deprecated usage `resources_dir` variable inside scripts, use `meta["resources_dir"]` instead (or `$meta_resources_dir` in Bash, or `meta$resources_dir` in R).

* Deprecated `meta["n_proc"]` in favour for `meta["cpus"]`.

## NEW FUNCTIONALITY

* `viash ns exec`: Added two more fields:

  - `{platform}`: the platform name (if applicable)
  - `{namespace}`: the namespace of the component

* `LongArgument`: Added support for 64-bit integers with `type: long` as opposed to `type: integer` which are 32-bit integers.

## MAJOR CHANGES

* Allow passing integers/doubles/booleans to string parameters (#225). Removed the 'Version' helper class.

## MINOR CHANGES

* `meta["cpus"]` is now an integer, `meta["memory_*"]` are now longs (#224).

* `DockerPlatform`: Only store author names in the authors metadata.

* `NextflowPlatform`: Only store author names in the authors metadata.

* `Argument[_]`: Turn `multiple_sep` from `Char` into `String`.

## INTERNAL CHANGES

* All `meta[...]` variables are now processed similar to `Argument[_]`s, instead of using custom code to convert object types and detect Docker mounts.

* `Escaper`: Make more generic Escaper helper class.

## DOCUMENTATION

* Hardcoded URLs pointing to viash.io in the documentation annotations were replaced with a new keyword system.

* Replaced references to "DSL" with "Dynamic Config Modding" in the `--help` output.

* Added an example for Ruby based Docker setups.

## BUG FIXES

* `viash ns`: Reverse exit code outputs, was returning 1 when everything was OK and 0 when errors were detected (PR #227).

* `viash config inject`: Fix processing of arguments when argument groups are defined (#231).

* Fixed a few typos in the CLI.

* Fixed the formatting of `ns exec` documentation.

* `VDSL3`: Fix stub functionality.

* `VDSL3`: Fix error during error message.

* `viash test`: Fix issue where `VIASH_TEMP` could not be a relative directory when running `viash test` (#242).

* `BashScript`, `CSharpScript`, `JavaScriptScript`, `PythonScript`, `RScript`, `ScalaScript`: Fix quoting issues of certain characters (#113).

## DEPRECATION

* `NextflowPlatform`: Deprecate `--param_list_format` parameter.

## TESTING

* `BashScript`, `CSharpScript`, `JavaScriptScript`, `PythonScript`, `RScript`, `ScalaScript`: Implement more rigorous testing of which characters are escaped.

* `BashWrapper`: Escape usage of `multiple_sep`. This fixes various checks and transformations not working when when `multiple_sep` is set to `";"` (#235).

# Viash 0.6.0 (2022-09-07): Nextflow VDSL3 is now the default, support for tracking memory and cpu requirements more elegantly

The first (major) release this year! The biggest changes are:

* Nextflow VDSL3 is now the default Nextflow platform, whereas the legacy Nextflow platform has been deprecated.

* Support for tracking memory and cpu requirements more elegantly.

* Grouping arguments in groups more concisely.

* The addition of a `viash ns exec` command, to be able to execute commands on Viash components more easily.

## BREAKING CHANGES

* `NextflowPlatform`: `variant: vdsl3` is now the default NextflowPlatform. `variant: legacy` has been deprecated.

* `Functionality`: Fields `.inputs` and `.outputs` has been deprecated. Please use `.argument_groups` instead (#186).
  Before:
  ```yaml
  functionality:
    inputs:
      - name: "--foo"
    outputs:
      - name: "--bar"
  ```
  Now:
  ```yaml
  functionality:
    argument_groups:
      - name: Inputs
        arguments:
          - name: "--foo"
            type: file
      - name: Outputs
        arguments:
          - name: "--bar"
            type: file
            direction: output
  ```

* Passing strings to an argument group's arguments has been deprecated. Please simply copy the argument itself into the argument group (#186).
  Before:
  ```yaml
  functionality:
    arguments:
      - name: "--foo"
        type: file
      - name: "--bar"
        type: file
        direction: output
    argument_groups:
      - name: Inputs
        arguments: [ foo ]
      - name: Outputs
        arguments: [ bar ]
  ```
  Now:
  ```yaml
  functionality:
    argument_groups:
      - name: Inputs
        arguments:
          - name: "--foo"
            type: file
      - name: Outputs
        arguments:
          - name: "--bar"
            type: file
            direction: output
  ```

## NEW FUNCTIONALITY

* Allow setting the number of processes and memory limit from within the Viash config, 
  as well as a list of required commands. Example:

  ```yaml
  functionality:
  name: foo
  requirements:
    cpus: 10
    memory: 10GB
    commands: [ bash, r, perl ]
  ```
  
  You can override the default requirements at runtime:

  - `./foo ---cpus 4 ---memory 100PB` (for NativePlatform or DockerPlatform)
  - By adding `process.cpus = 4` and `process.memory "100 PB"` to a nextflow.config (for NextflowPlatform)

  This results the following meta variables to be injected into a script:

  - `meta_cpus` (in Bash) or `meta["cpus"]` (in any other language): Number of processes the script is allowed to spawn.
  - `meta_memory_b` (in Bash) or `meta["memory_b"]` (in any other language): Amount of memory the script is allowed to allocate, in bytes.
  - `meta_memory_kb` (in Bash) or `meta["memory_kb"]` (in any other language): Same but in kilobytes, rounded up.
  - `meta_memory_mb` (in Bash) or `meta["memory_mb"]` (in any other language): Same but in megabytes, rounded up.
  - `meta_memory_gb` (in Bash) or `meta["memory_gb"]` (in any other language): Same but in gigabytes, rounded up.
  - `meta_memory_tb` (in Bash) or `meta["memory_tb"]` (in any other language): Same but in terabytes, rounded up.
  - `meta_memory_pb` (in Bash) or `meta["memory_pb"]` (in any other language): Same but in petabytes, rounded up.
  
* `viash ns exec`: Added a command for executing arbitrary commands for all found Viash components.
  The syntax of this command is inspired by `find . -exec echo {} \;`.
  
  The following fields are automatically replaced:
   * `{}` | `{path}`: path to the config file
   * `{abs-path}`: absolute path to the config file
   * `{dir}`: path to the parent directory of the config file
   * `{abs-dir}`: absolute path to the directory of the config file
   * `{main-script}`: path to the main script (if any)
   * `{abs-main-script}`: absolute path to the main script (if any)
   * `{functionality-name}`: name of the component
  
  A command suffixed by `\;` (or nothing) will execute one command for each
  of the Viash components, whereas a command suffixed by `+` will execute one
  command for all Viash components.

* `ConfigMod`: Added a `del(...)` config mod to be able to delete a value from the yaml. Example: `del(.functionality.version)`.

## MAJOR CHANGES

* `Folder structure`: Adjusted the folder structure to correctly reflect the the namespace change of viash from `com.dataintuitive.viash` to `io.viash`.

* `Functionality`: Reworked the `enabled` field from boolean to a `status` field which can have the following statusses: `enabled`, `disabled` and `deprecated`.
  When parsing a config file which has the `status` field set to `deprecated` a warning message is displayed on stderr.
  Backwards for `enabled` is provided where `enabled: true` => `status: enabled` and `enabled: false` => `status: false`. The `enabled` field is marked deprecated.

## MINOR CHANGES

* `Resources`: Handle edge case when no resources are specified in the `vsh.yaml` config file and display a warning message.

* `BashWrapper`: Add a warning when an argument containing flags (e.g. `--foo`) is not recognized and will be handled as a positional argument as this is likely a mistake.

* `Functionality`: Add check to verify there are no double argument names or short names in the config `vsh.yaml` declarations.

* `BashWrapper`: Add check to verify a parameter isn't declared twice on the CLI, except in the case `multiple: true` is declared as then it's a valid use case.

* `BashWrapper`: For int min/max checking: use native bash functionality so there is no dependency to `bc`.
  For double min/max checking: add fallback code to use `awk` in case `bc` is not present on the system (most likely to happen when running tests in a docker container).

* `viash ns list/viash config view`: Allow viewing the post-processed argument groups by passing the `--parse_argument_groups` parameter.

## TESTING

* `ConfigMod`: Added unit tests for condition config mods.

* `MainTestDockerSuite`: Derive config alternatives from the base `vsh.yaml` instead of adding the changes in separate files.
  This both reduces file clutter and prevents having to change several files when there are updates in the config format.

* `GitTest`: Added unit tests for Git helper (PR #216).

## BUG FIXES

* `csharp_script`, `javascript_script`, `python_script`, `r_script`, `scala_script`: Make meta fields for `memory` and `cpus` optional.

* `NextflowVdsl3Platform`: Don't generate an error when `--publish_dir` is not defined and `-profile no_publish` is used.

* `Viash run`: Viash now properly returns the exit code from the executed script.

* `Git`: Fix incorrect metadata when git repository is empty (PR #216).
