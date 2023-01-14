# Viash 0.6.8

## MINOR CHANGES

* `Main`: Capture build, setup and push errors and output an exit code.

* `Scala`: Updated to Scala 2.13 and updated several dependencies.

* `Testbenches`: Prepare ConfigDeriver by copying base resources to the targetFolder. Use cases so far showed that it's always required and it simplifies the usage.

* `Testbenches`: Remove some old & unmaintained IntelliJ Idea `editor-fold` tags. Given that the testbenches were split up, these were broken but also no longer needed.

* `Main`: Improve `match` completeness in some edge cases and throw exceptions where needed.
## BUG FIXES

* `Testbenches`: Simplify `testr` container.

# Viash 0.6.7

Another minor release which contains several quality of life improvements for the Nextflow VDSL3 platform, as well as automated warnings for deprecated functionality.

## MINOR CHANGES

* `NextflowPlatform`: Create directories during a stub run when output path is a nested directory (#314).

* Automatically generate a warning for deprecated parameters while parsing a .viash.yaml configuration file using the inline documentation deprecation annotations.

* Add a "planned removal" field in the deprecation annotations.

* Add testbenches to verify proper formatting of the deprecation versions and compare current version to the planned removal version so no deprecated parameters get to stick around beyond what was planned.

* `NextflowPlatform`: Nextflow processes are created lazily; that is, only when running
  a Nextflow workflow (#321).

## BUG FIXES

* `NextflowPlatform`: Automatically split Viash config strings into strings of 
  length 65000 since the JVM has a limit (65536) on the length of string constants (#323).


# Viash 0.6.6

This release fixes an issue where stderr was being redirected to stdout.

## BUG FIXES

* Don't redirect stderr to stdout when switching Viash versions (#312).

# Viash 0.6.5

A small update which fixes an issue with `viash ns list` that was
introduced in Viash 0.6.3.

## BUG FIXES

* `viash ns list`: When the `-p <platform>` is defined, filter the 
  output by that platform.

# Viash 0.6.4

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

* Config: Viash configs whose filenames start with a `.` are ignored (#291).

* `viash build`: `--write_meta/-m` and `--meta/-m` arguments have been removed. 
  Instead, the `.config.vsh.yaml` file is always created when building Viash components (#293).

* `FileArgument`: Default setting of `must_exist` was changed from `false` to `true`. 
  As such, the component will throw an error by default if an input file or output file
  is missing (#295).

* Config merging: `__inherits__` has been renamed to `__merge__`.

## NEW FUNCTIONALITY

* You can switch versions of Viash using the `VIASH_VERSION` 
  environment variable (#304)! Example:
  
  ```bash
  VIASH_VERSION=0.6.0 viash --version
  ```

* Traceability: Running `viash build` and `viash test` creates a `.config.vsh.yaml` file 
  by default, which contains the processed config of the component. As a side effect, 
  this allows for reading in the `.config.vsh.yaml` from within the component to learn 
  more about the component being tested (#291 and #293).

* `FileArgument`: Added `create_parent` option, which will check if the directory of an output
file exists and create it if necessary (#295).

## MINOR CHANGES

* `viash run`, `viash test`: When running or testing a component, Viash will add an extension
  to the temporary file that is created. Before: `/tmp/viash-run-wdckjnce`, 
  now: `/tmp/viash-run-wdckjnce.py` (#302).

* NextflowPlatform: Add `DataflowHelper.nf` as a retrievable resource in Viash (#301).

* NextflowPlatform: During a stubrun, argument requirements are turned off and
  the `publishDir`, `cpus`, `memory`, and `label` directives are also removed 
  from the process (#301).

* `NextflowPlatform`: Added a `filter` processing argument to filter the incoming channel after 
  the `map`, `mapData`, `mapId` and `mapPassthrough` have been applied (#296).

* `NextflowPlatform`: Added the Viash config to the Nextflow module for later introspection (#296).
  For example:
  ```groovy
  include { foo } from "$targetDir/path/foo/main.nf"

  foo.run(filter: { tup ->
    def preferredNormalization = foo.config.functionality.info.preferred_normalization
    tup.normalization_id == preferredNormalization
  })
  ```

## BUG FIXES

* `BashWrapper`: Don't overwrite meta values when trailing arguments are provided (#295).


## EXPERIMENTAL FEATURES

* Viash Project: Viash will automatically search for a `_viash.yaml` file in the directory of 
  a component and its parent directories (#294).

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


* Config merging: Allow specifying the order in which Viash will merge configs (#289).
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
  

# Viash 0.6.3

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

* `Config`: Any part of a Viash config can use inheritance to fill data (#271). For example:
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

# Viash 0.6.2

This is a quick release to push two bug fixes related to security and being able to run Nextflow with optional output files.

## BUG FIXES

* `Git`: Strip credentials from remote repositories when retrieving the path.

* `VDSL3`: Allow optional output files to be `null`.

# Viash 0.6.1

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

* `viash ns`: Reverse exit code outputs, was returning 1 when everything was OK and 0 when errors were detected (#227).

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

# Viash 0.6.0

The first (major) release this year! The biggest changes are:

* Nextflow VDSL3 is now the default Nextflow platform, whereas the legacy Nextflow platform has been deprecated.
* Support for tracking memory and cpu requirements more elegantly
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

* `GitTest`: Added unit tests for Git helper (#216).

## BUG FIXES

* `csharp_script`, `javascript_script`, `python_script`, `r_script`, `scala_script`: Make meta fields for `memory` and `cpus` optional.

* `NextflowVdsl3Platform`: Don't generate an error when `--publish_dir` is not defined and `-profile no_publish` is used.

* `Viash run`: Viash now properly returns the exit code from the executed script.

* `Git`: Fix incorrect metadata when git repository is empty (#216).

# Viash 0.5.15

## BREAKING CHANGES

* `WorkflowHelper::helpMessage`: Now only takes one argument, namely the config.

## MAJOR CHANGES

* `Namespace`: Changed the namespace of viash from `com.dataintuitive.viash` to `io.viash`.

## MINOR CHANGES

* `Testbenches`: Add a testbench framework to test lots of character sequences, single or repeating to be tested in the yaml config. This can be used to later extend to other tests.

* `Testbenches::vdsl3`: Add testbenches to verify functionality:
  - Vdsl3's `param_list` (`yamlblob`, `yaml`, `json`, `csv`).
  - NextFlow's own `params-file`.
  - Vdsl3's recalculating resource file paths to be relative to the `param_list` file instead of the workflow file (only available for `yaml`, `json`, `csv`).
  - Vdsl3's wrapping of modules to run these as a separate workflow automagically out of the box.

* `Main`: Added `viash --schema_export` which outputs a schema of the Viash config file
  to console. This is to be used to automate populating the documentation website.

* `Helper`: Split help message by argument group.

* `Helper`: Remove unneeded arguments.

* `Functionality`: Add default groups `Inputs`, `Outputs` and `Arguments` for all arguments missing from user-defined `argument_groups`.

* `WorkflowHelper::helpMessage`: Rewrite to bring on par with Viash's help message.

* `BooleanArguments`: Renamed internal class names for BooleanArguments to be better in line with how they are named in the config yaml.
  `BooleanArgumentRegular` -> `BooleanArgument` (in line with `boolean`)
  `BooleanArgumentTrue` -> `BooleanTrueArgument` (in line with `boolean_true`)
  `BooleanArgumentFalse` -> `BooleanFalseArgument` (in line with `boolean_false`)

## BUG FIXES

* `NextflowVdsl3Platform`: Change how `--id` is processed when a VDSL3 module is called from the CLI.

* `NextflowVdsl3Platform`: Fix error when param_list is `null`.

* `NextflowVdsl3Platform`: Fix error when optional, multiple arguments are set to `null`.

* `Testbenches`: Better capture expected error messages while running testbenches again. Code changes right before previous release re-introduced some of the messages.

* `NextflowVdsl3Platform`: Fix issue where optional parameters aren't removed when `.run(args: [optarg: null])`.

* `WorkflowHelper::readCsv`: Treat empty values as undefined instead of throwing an error.

* `NextflowVdsl3Platform`: Use `$NXF_TEMP` or `$VIASH_TEMP` as temporary directory if the container engine is not set to `docker`, `podman` or `charlieengine`, else set to `/tmp`.

* `Resources`: When adding a resource folder, allow a trailing `/` at the end of the path.
  Previously this caused the target folder to be erased and the content of the resource folder to be written directly into the target folder.

# Viash 0.5.14

## NEW FUNCTIONALITY

* `Functionality`: Allow specifying argument groups. Example:
  ```yaml
  functionality:
    ...
    argument_groups:
      - name: First group
        arguments: [foo, bar]
        description: Description
  ```


* Addition of the `viash_nxf_schema` component for converting a Viash config (for a workflow) into a nextflow schema file.

* `NextflowVdsl3Platform`: Use `--param_list` to initialise a Nextflow channel with multiple parameter sets.
  Possible formats are csv, json, yaml, or simply a yaml_blob.
  A csv should have column names which correspond to the different arguments of this pipeline.
  A json or a yaml file should be a list of maps, each of which has keys corresponding to the arguments of the pipeline.
  A yaml blob can also be passed directly as a parameter.
  Inside the Nextflow pipeline code, params.param_list can also be used to directly a list of parameter sets.
  When passing a csv, json or yaml, relative path names are relativized to the location of the parameter file.
  
  Examples: 
  ```sh
  nextflow run "target/foo/bar/main.nf" --param_list '[{"id": "foo", "input": "/path/to/bar"}]'
  nextflow run "target/foo/bar/main.nf" --param_list "params.csv" --reference "/path/to/ref"
  ```

## MAJOR CHANGES

* `NextflowVdsl3Platform`: The functionality is now slurped from a json instead of manually
  taking care of the formatting in Groovy.

* `NextflowVdsl3Platform`: The `--help` is auto-generated from the config.


## MINOR CHANGES

* `NextflowVdsl3Platform`: Allow both `--publish_dir` and `--publishDir` when `auto.publish = true`.

* `NextflowVdsl3Platform`: Allow passing parameters with multiplicity > 1 from Nextflow CLI.

* `Main`: Added `viash --cli_export` which outputs the internal cli construction information 
  to console. This is to be used to automate populating the documentation website.

* `viash ns`: Display success and failure summary statistics, printed to stderr.

* `DataObject`: `.alternatives` is now a `OneOrMore[String]` instead of `List[String]`, meaning
  you can now specify `{ type: string, name: "--foo", alternatives: "-f" }` instead of 
  `{ type: string, name: "--foo", alternatives: [ "-f" ] }`

* `BashWrapper`: Added metadata field `meta_executable`, which is a shorthand notation for
  `meta_executable="$meta_resources_dir/$meta_functionality_name"`

## INTERNAL CHANGES

* `Arguments`: Internal naming of functionality.arguments is changed from DataObject to Arguments. Change is also applied to child classes, e.g. StringObject -> StringArgument.

* `Script`: Allow more control over where injected code ends up.

* Restructure type system to allow type-specific arguments.

## BUG FIXES

* `DockerPlatform`: Change `org.opencontainers.image.version` annotation to `functionality.version` when set.
  Additionally fixed retrieving the git tag possibly returning `fatal: No names found, cannot describe anything.` or similar.

* `viash config inject`: Fix config inject when `.functionality.inputs` or `.functionality.outputs` is used.

* `BashWrapper`: Don't add `bc` as dependency. Only perform integer/float min/max checks when bc is available, otherwise ignore.

* `DockerPlatform`: Fix inputs & outputs arguments being present twice.

* `viash ns test`: Silently skip Nextflow platforms as these don't support tests and will always fail.

* `Testbenches`: Better capture expected error messages while running testbenches. Having these show on the console could be confusing.

* `NextflowVdsl3Platform`: Fix issue when running multiple VDSL3 modules concurrently on the same channel.


# Viash 0.5.13

## NEW FUNCTIONALITY

* `NextflowVdsl3Platform`: Allow overriding the container registry of all Viash components by 
  setting the `params.override_container_registry` value. Only works for auto-derived image names.

## MAJOR CHANGES

* `Functionality`: renamed `tests` to `test_resources`.
  Backwards compatibility provided but a notification message is displayed on the console.

## MINOR CHANGES

* `Functionality` and `viash ns`: Added `.enabled` in functionality, set to `true` by default.
  Filter for disabled components in namespace commands.

* `DockerPlatform`: Add org.opencontainers.image annotations to built docker images.

* `Functionality`: when defining text resources, permit defining `path` instead of `dest`.
  If both `dest` and `path` are unset, use a default file name depending on the resource type, such as `script.sh` or `text.txt`.

* `viash build`: Errors are printed in red.

## BUG FIXES

* `NextflowVdsl3Platform`: Undefined input files should not inject a `VIASH_PAR_*` variable when `multiple: true`.

* `NextflowVdsl3Platform`: Make injected resources dir absolute.

* `NextflowVdsl3Platform`: Fix escaping of triple single quotes.

* `NextflowVdsl3Platform`: Also apply auto.simplifyInput to Lists.

* `DockerPlatform`: added a `test_setup` that allows adding apt/apk/... setup requirements.
  These are only executed when running tests.

# Viash 0.5.12

## MINOR CHANGES

* `--help`: Don't print "my_component <not versioned>" when no version is specified, 
  but instead simply "my_component".

* `NextflowVdsl3Platform`: Set `mode=copy` for `auto.publish` and `auto.transcript`.

* `NextflowVdsl3Platform`: When a module is used multiple times in the same workflow, 
  don't throw an error anymore, instead simply generate a warning.

* `NextflowVdsl3Platform`: Throw an error when an input file was not found.

* `viash build`: Indent auto-generated code according the indentation of `VIASH START` when found.
  
* `Main`: Handle not finding the config file or resources in a config file better.
  Display a more helpful message instead of a stack trace.

* `BashWrapper`: Add checks on parameters for valid integer, double and boolean values.

* `BashWrapper`: Add option to limit string and integer values to specific choice values.

* `BashWrapper`: Add option to set min and max values for integer and double values.

* Dependencies:
  - Scala was upgraded from 2.12.10 to 2.12.15
  - sbt was upgraded from 1.3.4 to 1.6.1
  - sbt-scoverage was upgraded from 1.5.1 to 1.9.3

## BUG FIXES

* `viash_test`: Add back `--no_cache` parameter to `viash_test`.

* `viash_test`: Fix `--append` parameter for `viash_test`, was not getting passed through.

* `viash ns test`: Fix `--append` parameter, actually start from a clean file if append is false.

* `viash_push`: Fix component not being built during a release of Viash.

* `PythonRequirements`: Fix packages being mentioned twice in a Dockerfile.

* `Main`: Added support spaces in filenames of config files and resources

* `BashWrapper`: Display a message when the last parsed argument would require more values than are still available.
  Now display a message that values are missing, used to silently crash the wrapper.

* `viash config inject`: Fix error when file argument is `must_exist: true`.
  

# Viash 0.5.11

## MAJOR CHANGES

* `Functionality`: Now also accepts 'inputs' and 'outputs' in addition to 'arguments'. For inputs and outputs,
  any specified arguments will have default `type: file` and `direction: input` or `direction: output` respectively.

## MINOR CHANGES

* `DockerPlatform`: Move description labels to the end of the Dockerfile to improve cross-component caching.

* `Functionality`: Arguments where `.multiple` is `true` can now have lists as `default` and `example`.

* `viash_build`: Added unit test for this component.

* `viash_test`: Added unit test for this component.

* `PythonRequirements`: Allow upgrading dependencies. Example: `[ type: python. pypi: anndata, upgrade: true ]`.

* `NextflowLegacyPlatform`: Remove annoying messages when building Nxf modules.

* `ConfigMods`: Expanded the DSL to allow specifying at which point to apply a config mod.
  This functionality was necessary to allow for setting fields which alter the way configs are parsed.
  Example of when this is useful: `<preparse> .platforms[.type == "nextflow"].variant := "vdsl3"`.
  Updating workflow of parsing a config file is:
    - read Yaml from file
    - apply preparse config mods
    - parse resulting Json as Config, thereby instantiating default values etc.
    - convert Config back to Json
    - apply postparse config mods (original config mods)
    - convert final Json back to Config

## BETA FUNCTIONALITY

* `NextflowVdsl3Platform`: A beta implementation of the next-generation Viash+Nextflow platform.
  See https://github.com/viash-io/viash/issues/82 for more information. You can access the previous Nextflow
  platform by using the `variant` parameter:
  ```yaml
  - type: nextflow
    variant: legacy
    separate_multiple_outputs: false
  ```

## BUG FIXES

* `viash_build` and `viash_test`: The `query_name` and `query_namespace` arguments were switched around. These arguments are now passed correctly.

* `BashScript`, `JavaScriptScript`, `PythonScript`, `RScript`: Correctly escape `'` (#113). Update unit tests accordingly.

* `CSharpScript`, `ScalaScript`: Correctly escape `"` (#113). Update unit tests accordingly.

* `viash_build`, `viash_test`, `viash_push`: Don't try to remove log files if they don't exist.

## INTERNAL CHANGES

* `DataObject`: 
  - Renamed `otype` to `flags`.
  - Renamed `oType` to `type`
  - Deprecated `tag` (unused feature).

* All abstract / inherited classes: Renamed `oType` to `type`.

## DEPRECATION

* `Functionality`: Deprecated `function_type` and `add_resources_to_path`. These should be 
  unused features, by now.
  
# Viash 0.5.10.1

## BUG FIX

* `NextflowPlatform`: Fix passthrough of `organization` field.

# Viash 0.5.10

## MAJOR CHANGES

* `viash_install`:
  - Added `--log_prefix`: This prefix is used to determine the path of the log files for `viash_build`, `viash_test` and `viash_push`.
  - Added `--organization`: Id of the organisation to be used in the Docker image name, i.e. `<registry>/<organization>/<namespace><namespace_sep><name>`.
  - Added `--target_image_source`: Url to the Git repo in which this project resides.
  - Removed `--log`.

* `viash_build`:
  - Reduce code duplication by contructing the command with Bash Arrays.
  - Renamed `--platforms` to `--platform`.
  - Added `--organization`: Id of the organisation to be used in the Docker image name, i.e. `<registry>/<organization>/<namespace><namespace_sep><name>`.
  - Added `--target_image_source`: Url to the Git repo in which this project resides.
  - Changed default of `--log` from `log.txt` to `.viash_build_log.txt`.
  - Added `--verbose`: Print out the underlying `viash ns build` command before running it.

* `viash_test`:
  - Reduce code duplication by contructing the command with Bash Arrays.
  - Renamed `--platforms` to `--platform`.
  - Added `--organization`: Id of the organisation to be used in the Docker image name, i.e. `<registry>/<organization>/<namespace><namespace_sep><name>`.
  - Added `--target_image_source`: Url to the Git repo in which this project resides.
  - Changed default of `--log` from `log.txt` to `.viash_test_log.txt`.
  - Changed default of `--tsv` from `log.tsv` to `.viash_test_log.tsv`.
  - Added `--verbose`: Print out the underlying `viash ns test` command before running it.

* `viash_push`:
  - Reduce code duplication by contructing the command with Bash Arrays.
  - Added `--organization`: Id of the organisation to be used in the Docker image name, i.e. `<registry>/<organization>/<namespace><namespace_sep><name>`.
  - Changed default of `--log` from `log.txt` to `.viash_push_log.txt`.
  - Added `--verbose`: Print out the underlying `viash ns build` command before running it.

## MINOR CHANGES

* `NextflowPlatform`: Added the `organization` field to the nextflow platform as well.

# Viash 0.5.9

## NEW FEATURES

* `viash run`: A long running Viash component can be interrupted by pressing 
  CTRL-C or by sending it an `INT` or `SIGINT` signal.

* `DockerPlatform`: Automatically add a few labels based on metadata to Dockerfile.

* `DockerPlatform`: Added value `target_image_source` for setting the source of 
  the target image. This is used for defining labels in the dockerfile.
  Example:
  ```yaml
  target_image_source: https://github.com/foo/bar
  ```

## MINOR CHANGES

* `viash ns list`: Added `--format yaml/json` argument to be able to return the
  output as a json as well. Useful for when `jq` is installed but `yq` is not. Example:
  ```
    viash ns list -p docker -f json | jq '.[] | .info.config'
  ```

* `viash config view`: Same as above.

## DEPRECATION

* `CLI`: Deprecated `-P` flag use `-p` intead.

* `DockerPlatform`: Deprecated `version` value.

# Viash 0.5.8

## NEW FUNCTIONALITY

* `DockerPlatform`: Allow defining a container's organisation. Example:
  ```yaml
    - type: docker
      registry: ghcr.io
      organisation: viash-io
      image: viash
      tag: "1.0"
      target_registry: ghcr.io
      target_organization: viash-io
  ```

* `DockerRequirement`: Add label instructions. Example:
  `setup: [ [ type: docker, label: [ "foo BAR" ]]]`

* `Config`: In specific places, allow parsing a value as a list of values. Fixes #97.
  This mostly applies to list values in `DockerPlatform`, but also to author roles.
  Examples:
  ```yaml
  functionality:
    name: foo
    authors:
      - name: Alice
        role: author # can be a string or a list
  platforms:
    - type: docker
      port: "80:80" # can be a string or a list
      setup:
        - type: r
          packages: incgraph # can be a string or a list
  ```
  
## BREAKING CHANGES

* `viash test`: This command doesn't automatically add the resources dir to the path.

## BUG FIXES

* `Functionality`: Fix `.functionality.add_resources_to_path` not being picked up correctly.

* `AptRequirement`: Set `DEBIAN_FRONTEND=noninteractive` by default. This can be turned off by specifying:
  ```yaml
    - type: apt
      packages: [ foo, bar ]
      interactive: true
  ```

## MINOR CHANGES

* `Main`: Slightly better error messages when parsing of viash yaml file fails.
  Before:
  ```
  $ viash test src/test/resources/testbash/config_failed_build.vsh.yaml 
  Exception in thread "main" DecodingFailure(Unexpected field: [package]; valid fields: packages, interactive, type, List(DownField(apt), DownArray, DownField(platforms)))
  ```
  
  After:
  ```
  $ viash test src/test/resources/testbash/config_failed_build.vsh.yaml 
  Error parsing 'file:///path/to/viash/src/test/resources/testbash/config_failed_build.vsh.yaml'. Details:
  Unexpected field: [package]; valid fields: packages, interactive, type: DownField(apt),DownArray,DownField(platforms)
  ```


# Viash 0.5.7

## BREAKING CHANGES

* `viash config`: An argument's example now needs to be of the same type as the argument itself. 
  For example, `[ type: integer, name: foo, example: 10 ]` is valid, whereas 
  `[ type: integer, name: foo, example: bar ]` is not, as 'bar' cannot be cast to an integer.

## NEW FUNCTIONALITY

* `viash config inject`: A command for inserting a Viash header into your script.

* `YumRequirement`: Added a requirement setup for installing through yum. Example:
  `setup: [ [ type: yum, packages: [ wget] ] ]`

* `DockerRequirement`: Allow using copy and add instructions. Example:
  `setup: [ [ type: docker, add: [ "http://foo.bar ." ]]]`

## BUG FIXES

* `ViashTest`: Fix verbosity passthrough.

* `--help`: Fix repeated usage flag when printing the help.

# Viash 0.5.6

## BREAKING CHANGES

* `BashWrapper`: Forbidden flags `-v`, `--verbose`, `--verbosity` have been renamed to `---v`, `---verbose`, `---verbosity`.

## MINOR CHANGES

* Set version of helper scripts to the same version as Viash.

* `DockerPlatform`: Produce helpful warning message when Docker image can't be found remotely (#94).

* `DockerPlatform`: Produce helpful error message when Docker isn't installed or the daemon is not running (#94 bis).

## BUG FIXES

* `viash_install`:
  - Passing Viash path as a string instead of as a file to ensure the path is not converted to an absolute path
  - Switch from Docker backend to a Native backend, 'unzip' and 'wget' are required.
  - Correctly set the log file for viash_test.
  
* `DockerPlatform`: Added sleep workaround to avoid concurrency issue where a file is executed to
  build docker containers but apparently still in the process of being written.
  
* `DockerPlatform`: Fix order issue of ---verbose flag in combination with ---setup, allowing to run 
  `viash run config.vsh.yaml -- ---setup cb ---verbose` and actually get output.
  

# Viash 0.5.5

## BREAKING CHANGES

* `Functionality`: The resources dir no longer automatically added to the PATH variable. 
  To alter this behaviour, set `.functionality.add_resources_to_path` to `true`.

## MINOR CHANGES

* Bash Script: only define variables which have values.

* CSharp Test Component: Change Docker image to `dataintuitive/dotnet-script` to have more control over the lifecycle of 
  versioned tags.

* Updated Code of Conduct from v2.0 to v2.1.

## BUG FIXES

* Viash namespace: Fix incorrect output path when the parent directory of a Viash component is not equal to the value of
  `.functionality.name`.

# Viash 0.5.4

## BREAKING CHANGES

* `NextFlowPlatform`: The default caching mechanism is now what NextFlow uses as default. In order to replicate earlier
  caching, `cache: deep` should be specified in the Viash config file.

## NEW FEATURES

* `NextFlowPlatform`: Added `cache` directive to specify the typing of caching to be performed.

# Viash 0.5.3

## NEW FEATURES

* Similar to `par`, each script now also has a `meta` list. `meta` contains meta information about the component
  or the execution thereof. It currently has the following fields:
  - `meta["resources_dir"]`: Path to the directory containing the resources
  - `meta["functionality_name"]`: Name of the component

* `NextFlowPlatform`: Export `VIASH_TEMP` environment variable. 

## BUG FIXES

* `NextFlowPlatform`: Fix output formatting when `separate_multiple_outputs` is `false`.

# Viash 0.5.2

## MINOR CHANGES

* `DockerPlatform`: Added `run_args` field to allow setting `docker run` arguments.

* `NextFlowPlatform`: Added argument `separate_multiple_outputs` to allow not separating the outputs generated by a 
  component with multiple outputs as separate events on the channel.

## BUG FIX

* `IO`: Allow overwriting directory resources upon rebuild.

# Viash 0.5.1

## NEW FEATURES

* `CSharpScript`: Added support for C# scripts (`type: "csharp_script"`) to viash.

## MINOR CHANGES

* `NextFlowPlatform`: Added `directive_cpus`, `directive_max_forks`, `directive_memory` and `directive_time` parameters.

## BUG FIXES

* `BashWrapper`: Refactor escaping descriptions, usages, defaults, and examples (#34).

* `NextFlowPlatform`: Refactor escaping descriptions, usages, defaults and examples (#75).

* `NextFlowPlatform`: Add argument to output path to avoid naming conflicts for components with multiple output files (#76).

* `NextFlowPlatform`, `renderCLI()`: Only add flag to rendered command when boolean_true is actually true (#78).

* `DockerPlatform`: Only chown when output file exists.

## TESTING

* `viash build`: Capture stdout messages when errors are expected, so that they don't clutter the expected output.

* `viash build`: Check `--help` description output on the whole text instead of per letter or word basis.

* `TestingAllComponentsSuite`: Only testing bash natively, because other dependencies might not be available.

# Viash 0.5.0

## BREAKING CHANGES

* `DockerPlatform`: A Docker setup will be performed by default. Default strategy has been changed to `ifneedbepullelsecachedbuild` (#57).
  `---setup` strategy has been removed and `---docker_setup_strategy` has been renamed to `---setup`.
  This change allows running a component for the first time. During first time setup, the Docker container will be pulled or built automatically. 

* `NativePlatform`: Deprecated the native setup field.

## MAJOR CHANGES

* `NXF`: This version changes the handling logic for arguments. An argument can be either `required` or not and can have a `default: ...` value or not. Checks are implemented to verify that required arguments are effectively provided _during_ pipeline running.

* `NXF`: If one sticks to long-option argments in the viash config, for all arguments that are _required_, the way of specifying the arguments on the CLI is identical for the Docker and NextFlow platforms. Non-required arguments can still be accessed from CLI using `--<component_name>__<argument_name> ...`.

* `NXF`: Running a module as a standalone pipeline has become easier.

* `viash run`: Implement verbosity levels (#58). viash executables now have 7 levels of verbosity: emergency, alert, critical, error, warning, notice, info, debug.
  The default verbosity level is 'notice'. Passing `-v` or `--verbose` bumps up the verbosity level by one, `-vv` by two. The verbosity level can be set manually by passing `--verbosity x`.

## MINOR CHANGES

* `Docker Platform`: Added `privileged` argument, allowing to run docker with the `--privileged` flag.

* `Docker Requirements`: Allow specifying environment variables in the Dockerfile.

* Config modding: Added a `+0=` operator to prepend items to a list.

* `viash run`: Added a `--version` flag to viash executables for viewing the version of the component.

* `Functionality`: Added checks on the functionality and argument names.

* `viash run`: Added examples to functionality and arguments. Reworked `--help` formatting to include more information and be more consistent (#56).

## BUG FIXES

* `Docker R Requirements`: Install `remotes` when using `{ type: r, packages: [ foo ] }`.

* `config`: Throw error when user made a typo in the viash config (#62). 

## TESTING

* `NXF`: Add an end-to-end test for running a nextflow pipeline using viash components.

* `Docker`: Reorganized viash docker build testbench into a main testbench with smaller auxiliary testbenches to keep them more manageable and clear what happens where.

* `viash ns`: Added a basic testbench for namespace tests.


# Viash 0.4.0.1 (2021-05-12)

## BUG FIX

* `NXF`: Return original_params instead of updated params for now.

* `NXF`: Reinstate function_type: asis in line with the refactored module generation code

* `viash ns test`: print header when `--tsv foo.tsv --append true` but foo.tsv doesn't exist yet. Fixes #45.

# Viash 0.4.0 (2021-04-14)

## NEW FEATURES

* Config modding: A custom viash DSL allows overriding viash config properties at runtime. See online documentation for more information. Example:

```
 viash ns test \
  -p docker \
  -c '.functionality.version := "1.0.0"' \
  -c '.platforms[.type == "docker"].target_registry := "my.docker-registry.com"' \
  -c '.platforms[.type == "docker"].setup_strategy := "pull"' \
  -l
```

* `viash build`: The image can be pushed with `--push`. The same can be done by passing `---push` to 
  a viash executable.

* `viash ns` can query the name, namespace, or both, with the following arguments:
  - `--query_namespace` or `-n`: filter the namespace with a regex.
  - `--query_name`: filter the name with a regex.
  - `--query` or `-q`: filter the namespace/name with a regex.

* Added the `project_build`, `project_clean`, `project_push` and `project_test` components to this repository.

* Added a field `.functionality.info` of type `Map[String, String]` in order to be able to specify custom annotations to the component.

## BREAKING CHANGES

* `viash ns`: Argument `--namespace` has been renamed to `--query_namespace`.

* `viash ns`: Argument `--namespace` does not implicitly change the namespace of the functionality anymore. You can use the command DSL to reproduce this effect; for example: `-c '.functionality.namespace := "foo"'`.
  
* `Docker` & `NXF`: Attribute `version` is deprecated. Instead, the default value will be `.functionality.version`, which can be overridden by using the `tag` attribute.

* `NXF`: When running a viash component as a Nextflow module on its own, you now need to specify all input files on the command line. For instance, if `--input` and `--reference` are input file arguments, you need to start the process by running `nextflow run main.nf --input <...> --reference <...> <other arguments>`. Previously only the input file needed to be specified.
  
* `Docker` & `NXF`: Default separator between namespace and image name has been changed from `"/"` to `"_"`.

## MINOR CHANGES

* `Docker` & `NXF`: Parsing of image attributes for both `Docker` and `Nextflow` platforms are better aligned. You can define an image by specifying either of the following:
  - `{ image: 'ubuntu:latest' }` 
  - `{ image: ubuntu, tag: latest }`
  
* `Docker` & `NXF`: Allow changing the separator between a namespace and the image name.

## NEXTFLOW REFACTORING

The generation of Nextflow modules has been refactored thoroughly.
  
* `NXF`: The implicitly generated names for output files/directories have been improved leading to less clashes.

* `NXF`: Allow for multiple output files/directories from a module while keeping compatibility for single output. Please [refer to the docs](http://www.data-intuitive.com/viash_docs/config/platform-nextflow/#multiple-outputs).

* `NXF`: Allow for zero input files by means of passing an empty list `[]` in the triplet

* `NXF`: Remove requirement for `function_type: todir`

* `NXF`: It is now possible to not only specify `label: ...` for a nextflow platform but also `labels: [ ...]`.
  
## BUG FIXES

* Allow quotes in functionality descriptions.

* `NXF`: Providing a `default: ...` value for output file arguments is no longer necessary.


# Viash 0.3.2 (2021-02-04)

## BREAKING CHANGES

* `viash build`: Do not automatically generate a viash.yaml when creating an executable. 
  Instead, you need to add the `-w|--write_meta` flag in order to let viash know that it
  should generate a viash.yaml in the resources dir.

## MAJOR CHANGES

* `NXF`: Add beta functionality for running viash tests in Nextflow.

## MINOR CHANGES

* Resources: Rework the way resources paths are converted to absolute URIs, should not have any impact on UX.

## BUG FIXES

* `NXF`: Add temporary workaround for determining the used image name when running a component.

* Docker Platform: Set default setup strategy to "alwayscachedbuild" as this used to be the default viash behaviour.

* `NXF`: Fix issue where resource dir would not get mounted depending on which inputs are provided.

* `NXF`: Accept multiple inputs when component is running as standalone.

# Viash 0.3.1 (2021-01-26)

## NEW FEATURES

* Functionality: Added list of authors field. Example:

```yaml
functionality:
  authors:
    - name: Bob Cando
      roles: [maintainer, author]
      email: bob@cando.com
      props: {github: bobcando, orcid: XXXAAABBB}
```

* `Docker`: Allow specifying the registry with `target_registry`. Example:

```yaml
- type: docker
  image: bash:4.0
  target_registry: foo.io
  target_image: bar
  target_tag: 0.1
```

* `Docker`: `version` is now a synonym for `target_tag`.
  If both `version` and `target_tag` are not defined, `functionality.version` will
  be used instead.
  
* `Docker`: Can change the Docker Setup Strategy by specifying
  - in the yaml: `setup_strategy: xxx`
  - on command-line: `---docker_setup_strategy xxx` or `---dss xxx`
  
  Supported values for the setup strategy are:
  - alwaysbuild / build: build the image from the dockerfile (DEFAULT)
  - alwayscachedbuild / cachedbuild: build the image from the dockerfile, with caching
  - alwayspull / pull: pull the image from a registry
  - alwayspullelsebuild / pullelsebuild: try to pull the image from a registry, else build it
  - alwayspullelsecachedbuild / pullelsecachedbuild: try to pull the image from a registry, else build it with caching
  - ifneedbebuild: if the image does not exist locally, build the image
  - ifneedbecachedbuild: if the image does not exist locally, build the image with caching
  - ifneedbepull: if the image does not exist locally, pull the image
  - ifneedbepullelsebuild: if the image does not exist locally, pull the image else build it
  - ifneedbepullelsecachedbuild: if the image does not exist locally, pull the image else build it with caching
  - donothing / meh: do not build or pull anything
  
## MAJOR CHANGES

* License: viash is now licensed under GPL-3.

## MINOR CHANGES

* CLI: Allow parameters before and after specifying a viash config yaml. For example, 
  both following commands now work. Up until now, only the latter would work.
  - `viash run config.vsh.yaml -p docker`
  - `viash run -p docker config.vsh.yaml`

* Functionality: Arguments field can now be omitted.

* Scripts: Wrapped scripts now contain a minimal header at the top.

## BUG FIXES

* `NXF viash build`: Do not assume each config yaml has at least one test.

* Scripts: Fix Docker `chown` failing when multiple outputs are defined (#21).

* JavaScriptRequirements: Fix type getting set to "python" when unparsing.

* `viash run . ---debug`: Debug session should now work again

* Native `---setup`: Fix missing newlines when running native ---setup commands.

* Main: Fix crashing when no arguments are supplied.

* Namespace: Show error message when the config file can't be parsed.

* Executable resource: Fix Docker automount handling for Executable resources.

## TESTING

* YAML: Test invertibility of parsing/unparsing config objects.


# Viash 0.3.0 (2020-11-24)

## BREAKING CHANGES

* File format `functionality.yaml` is no longer supported. Use `config.vsh.yaml` or `script.vsh.R/py/...` instead.

* `viash run` and `viash test`: By default, temporary files are removed when the execution succeeded, otherwise they are kept. 
  This behaviour can be overridden by specifying `--keep true` to always keep the temporary files, and `--keep false` to always remove them.

* `NXF`: `function_type: todir` now returns the output directory on the `Channel` rather than its contents.

## NEW FEATURES

* Added `viash ns test`: Run all tests in a particular namespace. For each test, the exit code and duration is reported. Results can be written to a tsv file.
* Added support for JavaScript scripts.
* Added support for Scala scripts.
* `NXF`: publishing has a few more options:
  - `publish`: Publish or yes (default is false)
  - `per_id`: Publish results in directories containing the unique (sample) ID (default is true)
  - `path`: A prefix path for the results to be published (default is empty)
* Functionality resources and tests: Allow copying whole directories instead of only single files. Also allow to rename the destination folder by specifying a value for 'dest'.
* Platform R / Python dependencies: Allow running a simple command.

## MAJOR CHANGES

* The `-P <platform>` parameter will be deprecated. For now, all `-P` values are simply passed to `-p`.
* `viash ns build` and `viash ns test`: Now use all available platforms if `-p` is not specified.
* By default, python packages will not be installed as user. Use `user: true` to modify this behaviour.

## MINOR CHANGES

* Name of autogenerated Docker image is now `ns/tool`.
* Internal changes to make it easier to extend viash with more scripting languages.
* `NXF`: Default image is now `ns/tool` for consistency.
* `NXF`: Repurpose `asis` function type for having simple publishing steps (see docs).
* `NXF`: Add component name to main `process` name
* R dependencies: by default, do not reinstall Bioconductor packages. Set `bioc_force_install: true` to revert this behaviour.

## BUG FIXES

* `viash build`: Do not display error messages when pwd is not a git repository.

## TESTING

* `viash test`: Add tests for `viash test` functionality.


# Viash 0.2.2 (2020-09-22)

* MINOR CHANGE: Allow generating placeholder without VIASH START/VIASH END blocks.
* BUG FIX `viash ns build`: Some platforms would sometimes not be detected.
* BUG FIX `viash run`: Avoid error when no arguments need to be chowned.

# Viash 0.2.1 (2020-09-11)

* NEW FEATURE `NXF`: Data references in Map form can now have values being lists. In other words, we can have multiple options which have one or more values.
* NEW FEATURE `viash ns build`: Added --parallel and --setup flag.
* NEW FEATURE `viash build`: Added --setup flag.
* NEW FEATURE: Allow changing the order of setup commands using the `setup:` variable.
* NEW (HIDDEN) FEATURE: Do not escape `${VIASH_...}` elements in default values and descriptions!
* MINOR CHANGE: Remove `---chown` flag, move to `platform.docker.chown`; is set to true by default.
* MINOR CHANGE: Perform chown during both run and test using a Docker platform.
* BUG FIX: Issue trying to parse positional arguments even when none is provided.

# Viash 0.2.0 (2020-09-01)

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

# Viash 0.1.0 (2020-05-14)
* MAJOR CHANGES: Refactoring of the Functionality class as discussed in VIP1 (#1). This has resulted in a lot of internal changes, but the changes with regard to the yaml definitions are relatively minor. See the section below for more info.
* MINOR CHANGES: Updated the functionality.yamls under `atoms/` and `src/test/` to reflect these aforementioned changes.
* BUG FIX: Do not quote passthrough flags.
* BUG FIX: Allow for spaces inside of Docker volume paths.
* DOCUMENTATION: Updated the README.md.
* DOCUMENTATION: Provide some small examples at `doc/examples`.
* MINOR CHANGES: Allow for bioconductor and other repositories in the R environment.
* MINOR CHANGES: Add support for pip versioning syntax.

## Changes to functionality.yaml
* ftype has been renamed to function_type. The value for this field is also being checked.
* platform has been removed.
* Instead, the first resource listed is expected to have `type: r_script`, `type: bash_script`, `type: python_script`, or `type: executable`. The other resources are expected to have `type: file` by default, and are left untouched by Viash.
* in the arguments, field `flagValue` has been removed. Instead, use `type: boolean_true` and `type: boolean_false` to achieve the same effect.

## Changes to platform_(docker/native).yaml
* The `r: packages:` field has been renamed to `r: cran:`.

# Viash 0.0.1 (2020-05-05)
* Initial proof of concept.
