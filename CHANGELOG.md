# Viash 0.x.x (yyyy-MM-dd): TODO Add title

TODO add summary

## NEW FUNCTIONALITY

* `ExecutableRunner`: Add a `---docker_image_id` flag to view the Docker image ID of a built executable (PR #741).

* `viash ns query`: Add a query filter that allows selecting a single component by its path in a namespace environment (PR #744).

* `config schema`: Add `label` & `summary` fields for Config, PackageConfig, argument groups, and all argument types (PR #743).

## MINOR CHANGES

* `ExecutableRunner`: Make Docker runtime arguments configurable via the `---docker_run_args` argument (PR #740).

## BUG FIXES

* `platforms`: Re-introduce the `--platform` and `--apply_platform` arguments to improve backwards compatibility (PR #725).
  When the argument is used, a deprecation warning message is printed on stderr.
  Cannot be used together with `--engine` or `--runner` and/or `--apply_engine` or `--apply_runner`.

* `nextflow_runner`: Fix refactoring error in the `findStates()` helper function (PR #733).

* `viash ns exec`: Fix "relative fields" outputting absolute paths (PR# 737). Additionally, improve path resolution when using the `--src` argument.

# Viash 0.9.0-RC6 (2024-06-17): Hotfix for docker image name generation

Fix an issue where docker image names were not generated correctly.

## BUG FIXES

* `docker_engine`: Fix a bug in how the namespace separator is handled (PR #722).

# Viash 0.9.0-RC5 (2024-06-13): Improvements for CI

Dependencies now use `vsh` as the default organization level. This means that the organization level is now optional in the `repo` field of the dependencies.
Improved how the docker image name is generated to be more predictable.

## MINOR CHANGES

* `resources_test`: This field is removed again from the `_viash.yaml` as it was decided to impliment this temporary functionality using the `info` field (PR #711).

* `docker_engine`: Deprecate `registry`, `organization` and `tag` fields in the `docker_engine` (PR #712). Currently these are hardly ever used and instead the `image` field is used to specify the full image name.

* `docker_engine`: Add `target_package` field to the `docker_engine` (PR #712). This field, together with the `target_organization` is used to specify the full built container image name. The fields use proper fallback for the values set in the component config and package config.

* `organization`: Remove the `organization` field from the component config (PR #712). The value is now directly used by the `docker_engine` as a fallback from the `target_organization` field.

## BUG FIXES

* `build_info`: Correctly set the `.build_info.executable` to `main.nf` when building a component with a Nextflow runner (PR #720).

* `vsh organization`: ViashHub repositories now use `vsh` as the default organization (PR #718).
  Instead of having to specify `repo: vsh/repo_name`, you can now just specify `repo: repo_name`, which is now also the prefered way.

* `testbenches`: Add a testbench to verify dependencies in dependencies from scratch (PR #721).
  The components are built from scratch and the dependencies are resolved from the local repositories.

# Viash 0.9.0-RC4 (2024-05-29): Improvements for CI

These are mainly improvements for issues highlighted by running Viash in a CI environment.
Additionally, implemented a proper way of caching dependency repositories. The cache is stored under `~/.viash/repositories`.

## NEW FUNCTIONALITY

* `dependencies`: GitHub and ViashHub repositories now get properly cached (PR #699).
  The cache is stored in the `~/.viash/repositories` directory using sparse-checkout to only fetch the necessary files.
  During a build, the cache is checked for the repository and if it is found and still up-to-date, the repository is not cloned again and instead the cache is copied to a temporary folder where the files are checked out from the sparse-checkout.

* `resources_test`: Add a `resources_test` field to the `_viash.yaml` to specify resources that are needed during testing (PR #709).
  Currently it is up to the user or CI to make sure these resources are available in the `resources_test` directory during testing.

## BUG FIXES

`dependencies`: Fix resolving of dependencies of dependencies (PR #701). The stricter build config was now lacking the necessary information to resolve dependencies of dependencies.
  We added it back as `.build_info.dependencies` in a more structured, anonymized way.

`dependencies`: Fix the `name` field of repositories possibly being outputted in the build config (PR #703).

`symlinks`: Allow following of symlinks when finding configs (PR #704). This improves symlink functionality for `viash ns ...` and dependency resolving.

# Viash 0.9.0-RC3 (2024-04-26): Various bug fixes and minor improvements

Mainly fixes for code changes from previous release candidates. Some additional minor fixes and QoL improvements are included.

## BREAKING CHANGES

* `computational requirements`: Use 1000-base units instead of 1024-base units for memory (PR #686). Additionally, the memory units `kib`, `mib`, `gib`, `tib`, and `pib` are added to support 1024-base definitions.

## MINOR CHANGES

* `error message`: Improve the error message when using an invalid field in the config (#PR #662). The error message now includes the field names that are not valid if that happens to be the case or otherwise a more general error message.

* `config mods`: Improve the displayed error message when a config mod could not be applied because of an invalid path (PR #672).

## BUG FIXES

* `export json_schema`: Fix minor inconsistencies and make the strict schema stricter by adapting to what Viash will effectively return (PR #666).

* `deprecation & removal warning`: Improve the displayed warning where a deprecated or removed field could display a double '.' when it field was located at the root level (PR #671).

* `resource path`: Don't finalize the `path` field of a resource until it's written as part of building a component (PR #668).

* `requirements`: Improve the error message when a Python or R requirement uses a single quote in the `.script` field (PR #675).

* `viash test`: Fix Docker id between build and test components not being consistent when using a custom Docker registry (PR #679).

* `MainNSBuildNativeSuite`: Capture the error message when reading the configs so we can capture the expected warning message (PR #688).
  While almost all tests were already cleanly capturing their expected warning/error messages, this one was still remaining, resulting in warnings being shown in the output.

* `runners & engines`: When applying a filter on empty runners or engines, the fallback default `native engine` and `executable runner` respectively are set before applying the filter (PR #691).

# Viash 0.9.0-RC2 (2024-02-23): Restructure the config and change some default values

The `.functionality` layer has been removed from the config and all fields have been moved to the top layer.
The default `multiple_sep` has been changed from `:` to `;` to avoid conflicts with paths like `s3://foo/bar`.

## BREAKING CHANGES

* `arguments`: Change default `multiple_sep` from `:` to `;` to avoid conflicts with paths like `s3://foo/bar` (PR #645).
  The previous behaviour of using `multiple_sep: ":"` can be achieved by adding a config mod to the `_viash.yaml`:
  ```yaml
  config_mods: |
    .functionality.argument_groups[true].arguments[.multiple == true].multiple_sep := ":"
  ```

* `functionality`: Remove the `functionality` layer from the config and move all fields to the top layer (PR #649).

## MINOR CHANGES

* `package config`: Renamed `project config` to `package config` (PR #636). Now that we start using the config more, we came to the conclusion that "package" was better suited that "project".

* `ns exec`: Added an extra field `{name}` to replace `{functionality-name}` (PR #649). No immediate removal of the old field is planned, but it is deprecated.

* `BashWrapper`: Added meta-data field `meta_name` as a replacement for `meta_functionality_name` (PR #649). No immediate removal of the old field is planned, but it is deprecated.

## BUG FIXES

* `schema`: Don't require undocumented fields to set default values and add the `links` and `reference` fields to functionality as they were not meant only to be in the project config (PR #636).

# Viash 0.9.0-RC1 (2024-01-26): Restructure platforms into runners and engines

This release restructures the `platforms` field into `runners` and `engines`.
Additionally changes are made to sanitize the built config output and include additional relevant meta data.

## BREAKING CHANGES

* `runners` and `engines`: The usage of `platforms` is deprecated and instead these are split into `runners` and `engines` (PR #510). 
  The `platforms` field is still supported but will be removed in a future release.
  In brief, the `native platform` became a `native engine` and `docker platform` became a `docker engine`.
  Additionally, the `native platform` and `docker platform` became a `executable runner`, `nextflow platform` became a `nextflow runner`.
  The fields of `docker platform` is split between `docker engine` and `docker runner`: `port`, `workdir`, `setup_strategy`, and `run_args` (set to `docker_run_args`) are captured by the `runner` as they define how the component is run. The other fields are captured by the `engine` as they define the environment in which the component is run. One exception is `chown` which is rarely set to false and is now always enabled.

* `arguments`: Merge arguments into argument_groups during a json decode prepare step (PR #574). The `--parse_argument_groups` option from `ns list` and `config view` is deprecated as it is now always enabled.

## NEW FUNCTIONALITY

* `export json_schema`: Add a `--strict` option to output a subset of the schema representing the internal structure of the Viash config (PR #564).

* `config view` and `ns list`: Do not output internal functionality fields (#564). Additionally, add a validation that no internal fields are present when reading a Viash config file.

* `project config`: Add fields in the project config to specify default values for component config fields (PR #612). This allows for a more DRY approach to defining the same values for multiple components.

## MINOR CHANGES

* `testbenches`: Add testbenches for local dependencies (PR #565).

* `testbenches`: Refactor testbenches helper functions to uniformize them (PR #565).

* `logging`: Preserve log order of StdOut and StdErr messages during reading configs in namespaces (PR #571).

* `Java 21 support`: Update Scala to 2.13.12 and update dependencies (PR #602).

* `project config`: Output the project config under the default name `ProjectConfig` instead of `Project` during schema export (PR #631). This is now important as the project config is now part of the component config. Previously this was overridden as the class name was `ViashProject` which was less descriptive.

## BUG FIXES

* `__merge__`: Handle invalid yaml during merging (PR #570). There was not enough error handling during this operation. Switched to the more advanced `Convert.textToJson` helper method.

* `config`: Anonymize paths in the config when outputting the config (PR #625).

# Viash 0.8.7 (yyyy-MM-dd): TODO Add title

## BUG FIXES

* `viash build`: Fix error handling of non-generic errors in the build process or while pushing docker containers (PR #696).

# Viash 0.8.6 (2024-04-26): Bug fixes and improvements for CI

Fix some issues in some edge cases.
Add options for testing in a CI environment. Given that these options are not meant for general use, they are hidden from the help message.
Some improvements are made to run in Nextflow Fusion.

## DOCUMENTATION

* `docker setup strategy`: Fix inconsistencies in the documentation (PR #657).

* `repositories`: Fix `uri` -> `repo` in the repositories documentation (PR #682).

## NEW FUNCTIONALITY

* `viash test` and `viash ns test`: Add a hidden `--dry_run` option to build the tests without executing them (PR #676).

* `viash test` and `viash ns test`: Add a hidden `--deterministic_working directory` argument to use a fixed directory path (PR #683).

* `component names`: Verify that component namespace and name combinations are unique (PR #685).

## BUG FIXES

* `NextflowPlatform`: Fix publishing state for output arguments with `multiple: true` (#638, PR #639). 

* `Executable`: Check whether a multiple output file argument contains a wildcard (PR #639).

* `NextflowPlatform`: Fix a possible cause of concurrency issues (PR #669).

* `Resources`: Fix an issue where if the first resource is not a script, the resource is silently dropped (PR #670).

* `Docker automount`: Prevent adding a trailing slash to an automounted folder (PR #673).

* `NextflowPlatform`: Change the at-runtime generated nextflow process from an in-memory to an on-disk temporary file, which should cause less issues with Nextflow Fusion (PR #681).

# Viash 0.8.5 (2024-02-21): Bug fixes and documentation improvements

Fix a bug when building a test docker container which requires a test resource. Additional improvements for the website documentation and support for the latest version of Nextflow are added.

## BUG FIXES

* `NextflowPlatform`: Fix an issue with current nextflow-latest (24.01.0-edge) where our supporting library passes a GString instead of a String and results in a type mismatch (PR #640).

* `test resources`: Make non-script test resources available during building of a docker container for `viash test` (PR #652).

## DOCUMENTATION

* `repositories`: Improve the repository documentation with links and a overview table with links (PR #643).

# Viash 0.8.4 (2024-01-15): Bug fix

Fix building components with dependencies that have symlinks in their paths.

## BUG FIXES

* `dependencies`: Fix dependencies with paths using symlinks (PR #621). The resolution for the `.build.vsh` was changed to use the `toRealPath` previously, so dependency resolution must take account possible resolved symlinks too.

# Viash 0.8.3 (2024-01-08): Bug fixes

Fix some edge cases and improve usability.

## BUG FIXES

* `NextflowPlatform`: properly resolve paths when a nextflow workflow has another nextflow
  workflow as dependency and the worktree contains a directory that is a symlink (PR #614).

* `Main`: Fixes a bug added by #294 which causes Viash to print a stacktrace instead of a helpful error message when `viash` is run without any arguments (#617, PR #618).
  Thanks @mberacochea for pointing out this oversight!
  
* `Dependency`: When an alias is defined, pass the alias as a key to the `.run()` function (#601, PR #606).

# Viash 0.8.2 (2023-12-14): Minor changes and bug fixes

This release fixes a few bugs regarding dependencies and how the Nextflow platform handles Paths.

## MINOR CHANGES

* `NextflowTestHelper`: Do not hardcode a version of Nextflow in the testbench, 
  but use the version of Nextflow that is installed on the system (PR #593).

* GitHub Actions: Test different versions of Nextflow (22.04.5, latest, and latest-edge) (PR #593).
  Testing the latest Edge version of Nextflow will allow us to catch notice changes in Nextflow earlier.

* Updates to the documentation and templates in the Git repo (#598, PR #600):

  - Add contributing guidelines.

  - Add issue templates.

  - Reworked the pull request template.

## BUG FIXES

* `config`: Fix the main level of a component config not enforcing strict mode and instead allowing any field to be specified (PR #585).

* `dependencies`: Allow the user to define a local dependency with specifying `repository: local` as sugar syntax (PR #609). A local repository is the default value so it's not required to be filled in, but allowing it with a sensible sugar syntax makes sense.

* `Repositories`: Fix a structural issue where a repository defined directly in a `dependency` would require the `name` field to be set (PR #607). Repository variants are created with and without the `name` field. Repositories under `.functionality.dependencies[]` use repositories without the `name` field, while repositories under `.functionality.repositories[]` use repositories with the `name` field.

* `NextflowPlatform`: Do not resolve remote paths relative to the --param_list file (PR #592).

* `NextflowPlatform`: Allow finding `.build.yaml` file when worktree contains a directory that is a symlink (PR #611). 

# Viash 0.8.1 (2023-11-20): Minor bug fix to Nextflow workflows

This release fixes a bug in the Nextflow platform where calling a workflow with the `.run()` function without specifying the `fromState` argument would result in an error when the input channel contained tuples with more than two elements.

## BUG FIXES

 `NextflowPlatform`: Fix error when using `.run()` without using `fromState` and the input channel holds tuples with more than two elements (PR #587).

# Viash 0.8.0 (2023-10-23): Nextflow workflows and dependencies

Nextflow workflows definitions are picked up by Viash and assembled into a functional Nextflow workflow, reducing the amount of boilerplate code needed to be written by the user.
It also adds a new `runIf` argument to the `NextflowPlatform` which allows for conditional execution of modules.
We added new 'dependencies' functionality to allow for more advanced functionality to be offloaded and re-used in components and workflows.

## BREAKING CHANGES

* `NextflowPlatform`: Changed the default value of `auto.simplifyOutput` from `true` to `false` (#522, PR #518). With `simplifyOutput` set to `true`, the resulting Map could be simplified into a `File` or a `List[File]` depending on the number of outputs. To replicate the previous behaviour, add the following config mod to `_viash.yaml`:

  ```yaml
  config_mods: |
    .platforms[.type == 'nextflow'].auto.simplifyOutput := true
  ```

* `VDSL3Helper.nf`: Removed from the Viash jar file (PR #518). Its functions have been moved to `WorkflowHelper.nf`.

* `DataflowHelper.nf`: Added deprecation warning to functions from this file (PR #518).

* `preprocessInputs()` in `WorkflowHelper.nf`: Added deprecation warning to `preprocessInputs()` because this function causes a synchronisation event (PR #518).

* `author.props`: Removed deprecated `props` field (PR #536). Deprecated since 0.7.4.

## NEW FUNCTIONALITY

* `dependencies`: Add `dependencies` and `repositories` to `functionality` (PR #509). 
  The new functionality allows specifying dependencies and where to retrieve (repositories) them in a component, and subsequentially allows advanced functionality to be offloaded and re-used in scripts and projects. This is alike e.g. `npm`, `pip` and many others. A big difference is that we aim to provide the needed boilerplate code to ease the usage of the dependencies in scripts, workflows and pipelines.
  Note that the dependency is required to be a built Viash component or project and not a random file or code project found externally. This is needed to provide the necessary background information to correctly link dependencies into a component.

* `NextflowScript` & `NextflowPlatform`: Merged code for merging the `main.nf` files for VDSL3 components and wrapped Nextflow workflows (PR #518).
  By aligning the codebase for these two, wrapped Nextflow workflows are more similar to VDSL3 components. For example, you can override the behaviour of a
  wrapped Nextflow workflow using the `.run()` method. Status of a workflows `.run()` arguments:

  - Works as intended: `auto.simplifyInput`, `auto.simplifyOutput`, `fromState`, `toState`, `map`, `mapData`, `mapPassthrough`, `filter`, `auto.publish = "state"`
  - Does not work (yet): `auto.transcript`, `auto.publish = true`, `directives`, `debug`.

  In a next PR, each of the dependencies will have their values overridden by the arguments of the `.run`.

* `NextflowPlatform`: The data passed to the input of a component and produced as output by the component are now validated against the arguments defined in the Viash config (PR #518).

* `NextflowPlatform`: Use `stageAs` to allow duplicate filenames to be used automatigically (PR #518).

* `NextflowPlatform`: When wrapping Nextflow workflows, throw an error if the IDs of the output channel doesn't match the IDs of the input channel (PR #518).
  If they don't, the workflow should store the original ID of the input tuple in the in the `_meta.join_id` field inside the state as follows:
  Example input event: `["id", [input: file(...)]]`,
  Example output event: `["newid", [output: file(...), _meta: [join_id: "id"]]]`

* `NextflowPlatform`: Added new `.run()` argument `runIf` - a function that determines whether the module should be run or not (PR #553).
  If the `runIf` closure evaluates to `true`, then the module will be run. Otherwise it will be passed through without running.

## MAJOR CHANGES

* `WorkflowHelper.nf`: The workflow helper was split into different helper files for each of the helper functions (PR #518).
  For now, these helper files are pasted together to recreate the `WorkflowHelper.nf`.
  In Viash development environments, don't forget to run `./configure` to start using the updated Makefile.

* `NextflowPlatform`: Set default tag to `"$id"` (#521, PR #518).

* `NextflowPlatform`: Refactoring of helper functions (PR #557).
  - Cleaned up `processConfig()`: Removed support for `functionality.inputs` and `functionality.outputs`
  - Cleaned up `processConfig()`: Removed support for `.functionality.argument_groups[].argument` containing a list of argument ids as opposed to the arguments themselves.
  - Rewrote `--param_list` parser.
  - Removed unused function `applyConfig()` and `applyConfigToOneParamSet()`.
  - Refactored `channelFromParams()` to make use of new helper functions.
  - Removed deprecated `paramsToChannel()`, `paramsToList()`, `viashChannel()`.
  - Deprecated `preprocessInputs()` -- use the wrapped Viash Nextflow functionality instead.
  - Refactored `preprocessInputs()` to make use of new helper functions.
  - Deprecated run arguments `map`, `mapData`, `mapPassthrough`, `renameKeys`.

## MINOR CHANGES

* `NextflowPlatform`: Throw error when unexpected keys are passed to the `.run()` method (#512, PR #518).

* `Testbenches`: Add testbenches for the new `dependencies` functionality and other small coverage improvements (PR #524).

* `NextflowPlatform`: Use `moduleDir` instead of `projectDir` to determine the resource directory.

* `NextflowPlatform`: Rename internal VDSL3 variables to be more consistent with regular Viash component variables and avoid naming clashes (PR #553).

## DOCUMENTATION

* Minor fixes to VDSL3 reference documentation (PR #508).

## BUG FIXES

* `WorkflowHelper.nf`: Only set default values of output files which are **not already set**, and if the output file argument is **not required** (PR #514).

* `NextflowPlatform`: When using `fromState` and `toState`, do not throw an error when the state or output is missing an optional argument (PR #515).

* `export cli_autocomplete`: Fix output script format and hide `--loglevel` and `--colorize` (PR #544). Masked arguments are usable but might not be very useful to always display in help messages.

# Viash 0.7.5 (2023-08-11): Minor breaking changes and new features

This release contains minor breaking change due to deprecated or outdated functionality being removed.

New functionality includes:

  - Export a JSON schema for the Viash config with `viash export json_schema`

  - Export a Bash or Zsh autocomplete script with `viash export cli_autocomplete`

  - Nextflow VDSL3 modules now have a `fromState` and `toState` argument to allow for better control of the data that gets passed to the module and how the state is managed in a Nextflow workflow.

## BREAKING CHANGES

* `viash export cli_schema`: Added `--format yaml/json` argument, default format is now a YAML (PR #448).

* `viash export config_schema`: Added `--format yaml/json` argument, default format is now a YAML (PR #448).

* `NextflowLegacyPlatform`: Removed deprecated code (PR #469).

* `viash_*`: Remove legacy viash_build, viash_test and viash_push components (PR #470).

* `ComputationalRequirements`, `Functionality`, `DockerPlatform`, `DockerRequirements`: Remove documentation of removed fields (PR #477).

## NEW FUNCTIONALITY

* `viash export json_schema`: Export a json schema derived from the class reflections and annotations already used by the `config_schema` (PR #446).

* `viash export config_schema`: Output `default` values of member fields (PR #446).

* `CI`: Test support for different Java versions on GitHub Actions (PR #456). Focussing on LTS releases starting from 11, so this is 11 and 17. Also test latest Java version, currently 20.

* `viash test` and `viash ns test`: add `--setup` argument to determine the docker build strategy before a component is tested (PR #451).

* `viash export cli_autocomplete`: Export a Bash or Zsh autocomplete script (PR #465 & #482).

* `help message`: Print the relevant help message of (sub-)command when `--help` is given as an argument instead of only printing the help message when it is the leading argument and otherwise silently disregarding it (initially added in PR #472, replaced by PR #496). This is a new feature implemented in Scallop 5.0.0.

* `Logging`: Add a Logger helper class (PR #485 & #490). Allows manually enabling or disabling colorizing TTY output by using `--colorize`. Add provisions for adding debugging or trace code which is not outputted by default. Changing logging level can be changed with `--loglevel`. These CLI arguments are currently hidden.

* `NextflowPlatform`: Nextflow VDSL3 modules now have a `fromState` and `toState` argument to allow for better control of the data that gets passed to the module and how the state is managed in a Nextflow workflow (#479, PR #501).

## MINOR CHANGES

* `PythonScript`: Pass `-B` to Python to avoid creating `*.pyc` and `*.pyo` files on importing helper functions (PR #442).

* `viash config`: Special double values now support `+.inf`, `-.inf` or `.nan` values (PR #446 and PR #450). The stringified versions `"+.inf"`, `"-.inf"` or `".nan"` are supported as well. This is in line with the yaml spec.

* `system environment variables`: Add wrapper around `sys.env` and provide access to specific variables (PR #457). Has advantages for documentation output and testbenches.

* `testbench`: Added some minor testbenches to tackle missing coverage (PR #459, #486, #488, #489, #492 & #494).

* `viash export config_schema`: Simplify file structure (PR #464).

* `helpers.Format`: Add a helper for the Format helper object (PR #466).

* `testbench`: Use config deriver to create config variants for testing (PR #498). This reduces the amount of config files that need to be maintained.

## BUG FIXES

* `viash config`: Validate Viash config Yaml files better and try to give a more informative error message back to the user instead of a stack trace (PR #443).

* `viash ns build`: Fix the error summary when a setup or push failure occurs. These conditions were not displayed and could cause confusion (PR #447).

* `testbench`: Fix the viash version switch test bench not working for newer Java versions (PR #452).

* `malformed input exception`: Capture MalformedInputExceptions when thrown by reading files with invalid Ascii characters when unsupported by Java (PR #458).

* `viash project file parsing`: Give a more informative message when the viash project file fails to parse correctly (PR #475).

* `DockerPlatform`: Fix issue when mounting an input or output folder containing spaces (PR #484).

* `Config mod`: Fix a config mod where the filter should execute multiple deletes (PR #503).

## DOCUMENTATION

* `NextflowPlatform`: Add documentation for the usage and arguments of a VDSL3 module (PR #501).

## INTERNAL CHANGES

* `NextflowVDSL3Platform`: Renamed to `NextflowPlatform` (PR #469).

* Rename mentions of `NextFlow` to `Nextflow` (PR #476).

* `Reference static pages`: Move `.qmd` files from the website to a local folder here; `docs/reference` (PR #504). This way we can track behaviour changes that need to be documented locally.

# Viash 0.7.4 (2023-05-31): Minor bug fixes and minor improvements to VDSL3

Some small fixes and consistency improvements.
A few Quality of Life improvements were made e.g. to override the Docker `entrypoint` when working with Nextflow and providing default labels when building a Nextflow workflow.

## NEW FUNCTIONALITY

* Add default labels in Nextflow config files that set default values for cpu and memory settings (PR #412). Values are more or less logarithmically spaced (1, 2, 5, 10, ...).

* `Author`: Added `info` field to authors. Deprecated `props` field (PR #423).

* `viash config view` and `viash ns list`: Set the `.info.output` path when a platform argument is provided.

* `viash ns exec`: Added two more fields:

  - `{output}`: path to the destination directory when building the component
  - `{abs-output}`: absolute path to the destination directory when building the component

* `DockerPlatform`: Add `entrypoint` and `cmd` parameters to the docker platform config that allows overriding the default docker container settings (PR #432).

## MINOR CHANGES

* `Nextflow VDSL3`:
  - Add profiles to the Nextflow Config file when the main script is a `NextflowScript` (#408).
  - Add a `script` parameter in Nextflow Config file to add a single string or list of strings to the `nextflow.config` (PR #430).

* `Scripts`: Remove the `entrypoint` parameter for all script types except `NextflowScript` (#409). All these scripts had to check individually whether the parameter was unset, now it can be done in the `Script` apply method.

* `schema export`:
  - Restructure Nextflow-Directives, -Auto and -Config into a `nextflowParameters` group (PR #412). Previously only NextflowDirectives was exposed.
  - Restructure the format to group authors & computational requirements together with functionality (PR #426).
  - Restructure the Viash Config and Project Config pages under a `config` category (PR #426).
  - Add references in Functionality and Nextflow VDSL3 to the new documentation pages (PR #426).
  - Add description and/or examples for platforms and requirements (PR #428).

## BUG FIXES

* `viash config inject`: Fix an empty line being added at the script start for each time `viash config inject` was run (#377).

* `WorkflowHelper`: Fixed an issue where passing a remote file URI (for example `http://` or `s3://`) as `param_list` caused `No such file` errors.

* `BashWrapper`: Fix escaping of the included script where a line starting with a pipe character with optional leading spaces is stripped of the leading spaces and pipe character.
  This was quite unlikely to happen except when `viash config inject` was called on a Nextflow Script, which lead to no real config code being injected however workflows were getting corrupted. (#421)

* `Deprecation testbench`: Add missing classes to be checked (PR #426).

# Viash 0.7.3 (2023-04-19): Minor bug fixes in documentation and config view

Fix minor issues in the documentation and with the way parent paths of resources are printed a config view.

## BUG FIXES

* `DockerPlatform`: Fixed example in documentation for the `namespace_separator` parameter (PR #396).

* `viash config view`: Resource parent paths should be directories and not file (PR #398).


# Viash 0.7.2 (2023-04-17): Project-relative paths and improved metadata handling

This update adds functionality to resolve paths starting with a slash as relative to the project directory, improves handling of info metadata in the config, and fixes to the operator precedence of config mods.

## NEW FUNCTIONALITY

* Resolve resource and merge paths starting with a slash (`/`) as relative to the project directory (PR #380). To define absolute paths (which is not recommended anyway), prefix the path with the `file://` protocol. Examples:

  - `/foo` is a file or directory called `foo` in the current project directory.
  - `file:/foo` is a file or directory called `foo` in the system root.

## MINOR CHANGES

* `viash config view`: Do not modify (e.g. strip empty fields) of the `.functionality.info` and `.functionality.arguments[].info` fields (#386).

## BUG FIXES

* `ConfigMods`: Fix operator precedence issues with conditions in the config mod parsers (PR #390).

## INTERNAL CHANGES

* Clean up unused code (PR #380).

* Move circe encoders/decoders for File and Path from `io.viash.functionality.arguments` to `io.viash.helpers.circe` (PR #380).

* Store the project root directory (that is, the directory of the `_viash.yaml`) in a ViashProject object (PR #380).

* Tests: Reworked language tests to be grouped in their own subfolder and split off the bash language test from the general `testbash` folder (PR #381).

* Tests: Add additional language tests for `viash config inject` (PR #381).

* Tests: Added test for `io.viash.helpers.IO` (PR #380).


# Viash 0.7.1 (2023-03-08): Minor improvements to VDSL3 and schema functionality.

This is a minor release which improves caching in VDSL3 components and changes the formats of the schema files for the Viash config and CLI.

## MINOR CHANGES

* `DataflowHelper`: Add assertions and `def`s.

## BUG FIXES

* `VDSL3`: Only the first two elements from an event in a channel are now passed to a process. This avoids calculating cache entries based on arguments that are not used by the process, causing false-negative cache misses.

* `config_schema`:
  - Correct some incorrect markdown tags.
  - Add project config.
  - Correct documentation/markdown tags to the correct order.
  - Add summary description and example for 'resource' and 'argument', to be used on the reference website.
  - Add documentation for the Nextflow directives.

* `cli_schema`: Correct documentation/markdown tags to the correct order.

# Viash 0.7.0 (2023-02-28): Major code cleanup and minor improvements to VDSL3

* Default namespace separator has been changed from `_` to `/`. This means 
  Docker images will be named `<Registry>/<Organization>/<Namespace>/<Name>`
  by default. For example, `ghcr.io/openpipelines-bio/mapping/cellranger_count`
  instead of `ghcr.io/openpipelines-bio/mapping_cellranger_count`.

* Removed deprecated code of unused functionality to simplify code.
  - Shorthand notation for specitying input/output arguments
  - Shorthand notation for specifying Docker requirements
  - Legacy Nextflow platform

* Improvements in VDSL3 and the Nextflow Workflow Helper to make behaviour
  more predictable and fixing some bugs in the meantime. Run the following
  to get access to the updated helpers:

  ```bash
  WF_DIR="src/wf_utils"
  [[ -d $WF_DIR ]] || mkdir -p $WF_DIR
  viash export resource platforms/nextflow/ProfilesHelper.config > $WF_DIR/ProfilesHelper.config
  viash export resource platforms/nextflow/WorkflowHelper.nf > $WF_DIR/WorkflowHelper.nf
  viash export resource platforms/nextflow/DataflowHelper.nf > $WF_DIR/DataflowHelper.nf
  ```

* Improvements to test benches and several bug fixes.

## BREAKING CHANGES

* Viash config: Previously deprecated fields are now removed.
  - `functionality.inputs`: Use `arguments` or `argument_groups` instead.
  - `functionality.outputs`: Use `arguments` or `argument_groups` instead.
  - `functionality.tests`: Use `test_resources` instead. No functional difference.
  - `functionality.enabled`: Use `status: enabled` instead.
  - `functionality.requirements.n_proc`: Use `cpus` instead.
  - `platforms.DockerPlatform.privileged`: Add a `--privileged` flag in `run_args` instead.
  - `platforms.DockerPlatform.apk`: Use `setup: [{ type: apk, packages: ... }]` instead.
  - `platforms.DockerPlatform.apt`: Use `setup: [{ type: apt, packages: ... }]` instead.
  - `platforms.DockerPlatform.yum`: Use `setup: [{ type: yum, packages: ... }]` instead.
  - `platforms.DockerPlatform.r`: Use `setup: [{ type: r, packages: ... }]` instead.
  - `platforms.DockerPlatform.python`: Use `setup: [{ type: python, packages: ... }]` instead.
  - `platforms.DockerPlatform.docker`: Use `setup: [{ type: docker, run: ... }]` instead.
  - `platforms.DockerPlatform.docker.setup.resources`: Use `setup: [{ type: docker, copy: ... }]` instead.
  - `platforms.NextflowLegacy`: Use the Nextflow VDSL3 platform instead.
  - `functionality.ArgumentGroups`: No longer supports strings referring to arguments in the `arguments:` section.
    Instead directly put the arguments inside the argument groups.

* `viash_install`: The bootstrap script has been reworked in line with the project config introduced in 0.6.4:

    * The default location for installing the Viash executable is now `./viash` (was: `bin/viash`).
    * The new `viash_install` support `--output` and `--tag`.
    * The various settings that existed in `viash_install` (organisation, tag, ...) are moved to the project config.

  Please note that this new `viash_install` bootstrap script can be run from the CLI using:

    ```
    curl -fsSL dl.viash.io | bash
    ```
  The old `get.viash.io` is still available but points to the version 0.6.7 version of this component and is deprecated.

* `WorkflowHelper`: `paramsToList`, `paramsToChannel` and `viashChannel` are now deprecated and will be removed in a future release.

* `viash (ns) build`: Change the default value of the namespace separator in a Docker platform from `_` to `/`. 
  Add `".platforms[.type == 'docker'].namespace_separator := '_'"` to the project config `_viash.yaml` to revert to the previous behaviour.

## MAJOR CHANGES

* `VDSL3`: now uses the newly implemented `channelFromParams` and `preprocessInputs` instead of `viashChannel`.

## NEW FEATURES

* `WorkflowHelper`: Added `preprocessInputs` and `channelFromParams` to replace `paramsToList`, `paramsToChannel` and `viashChannel`. This refactor allows processing parameters that are already in a Channel using `preprocessInputs`, which is necessary when passing parameters from a workflow to a subworkflow in a Nextflow pipeline.

## MINOR CHANGES

* `Main`: Capture build, setup and push errors and output an exit code.

* `File downloading`: Add check to pre-emptively catch file errors (e.g. 404).

* `Scala`: Updated to Scala 2.13 and updated several dependencies.

* `Main`: Improve `match` completeness in some edge cases and throw exceptions where needed.

* `Changelog`: Reformat the changelog to a more structured format.
  For every release, there is now a date, title, and summary.
  This both improves the changelog itself but can then also be used to postprocess the CHANGELOG programmatically.

* `VDSL3`: Add a default value for `id` when running a VDSL3 module as a standalone pipeline.

* `TestBenches`:
  - Verify switching of Viash versions
  - Prepare ConfigDeriver by copying base resources to the targetFolder. Use cases so far showed that it's always required and it simplifies the usage.
  - Remove some old & unmaintained IntelliJ Idea `editor-fold` tags. Given that the testbenches were split up, these were broken but also no longer needed.
  - Add 2 testbenches for computational requirements when running `viash run` or `viash test`.
  - Added tests for different values for the `--id` and `--param_list` parameters of VDSL3 modules.

* `viash test`: Use `test` as a random tag during testing, instead of `test` plus a random string.

## BUG FIXES

* `WorkflowHelper`: fixed where passing a relative path as `--param_list` would cause incorrect resolving of input files.

* `Testbenches`: Fix GitTest testbench to correctly increment temporary folder naming and dispose them after the test finishes.

* `viash xxx url`: Fix passing a url to viash as the config file to process. Add a short testbench to test principle functionality.

* `Testbenches`: Simplify `testr` container.

* `Main`: Improve error reporting to the user in some cases where files or folders can't be found. Depending on the thrown exception, more or less context was given.

* `VDSL3`: Create parent directory of output files before starting the script.

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

# Viash 0.5.15 (2022-07-14): Added testbenches, default argument groups and bugfixes for VDSL3

This release introduces testbenches and new default argument groups: `Inputs`, `Outputs` and `Arguments`.

## BREAKING CHANGES

* `WorkflowHelper::helpMessage`: Now only takes one argument, namely the config.

## MAJOR CHANGES

* `Namespace`: Changed the namespace of viash from `com.dataintuitive.viash` to `io.viash`.

## MINOR CHANGES

* `Testbenches`: Add a testbench framework to test lots of character sequences, single or repeating to be tested in the yaml config. This can be used to later extend to other tests.

* `Testbenches::vdsl3`: Add testbenches to verify functionality:
  - Vdsl3's `param_list` (`yamlblob`, `yaml`, `json`, `csv`).
  - Nextflow's own `params-file`.
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

# Viash 0.5.14 (2022-06-30): Argument groups can now be defined in the Viash config

Argument groups allow for grouping arguments together by function or category, making the `--help` output a lot more clear for components with a lot of arguments.

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

# Viash 0.5.13 (2022-06-10): Added overriding of the container registry for the VDSL3 + VDSL3 bug fixes

VDSL3 gets even more improvements and bug fixes.

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

# Viash 0.5.12 (2022-05-24): Improvements for VDSL3 and the Bash wrapper + several bug fixes

This release contains a bunch improvements for VDSL3 and adds some parameters to the `viash test` and `viash test ns` commands.

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
  

# Viash 0.5.11 (2022-05-09): Nextflow VDSL3 is here!

This release contains additional sugar syntax for specifying inputs and outputs in a Viash config, 
a beta implementation for the next-generation Viash platform, and several other minor improvements.

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
  
# Viash 0.5.10.1 (2022-03-16): A quick bug fix

This quick release fixes a bug that prevented the correct passthrough of the new `organization` field.

## BUG FIX

* `NextflowPlatform`: Fix passthrough of `organization` field.

# Viash 0.5.10 (2022-03-15): Rework of the Viash helper components

The `viash_install`, `viash_build`, `viash_test` and `viash_push` components have been reworked.

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

# Viash 0.5.9 (2022-03-12): Allow interrupting Viash components

The biggest change in this release is that long running Viash components (VS Code server or R Studio server for example) can now be interrupted by pressing CTRL-C or by sending it an `INT` or `SIGINT` signal. Before this release, you had to manually stop the Docker container to get the component to terminate.

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

# Viash 0.5.8 (2022-02-28): Allow defining a Docker image organization, and single values can be used in place of lists

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
  Error parsing 'file:/path/to/viash/src/test/resources/testbash/config_failed_build.vsh.yaml'. Details:
  Unexpected field: [package]; valid fields: packages, interactive, type: DownField(apt),DownArray,DownField(platforms)
  ```

# Viash 0.5.7 (2022-02-16): Argument examples need to be of the same type as the argument itself

Examples for arguments now need to be of the same type as the argument itself. You can't provide an `integer` for a `string`-based argument for example.  
A handy new command has been added: `viash config inject`. This can be used to inject a Viash header into a script based on the arguments of the config file.

There have been some improvements to the Docker platform as well.  
You can now add yum packages as a requirement:

  ```yaml
  platforms:
    - type: docker
      image: bash:latest
      setup:
        - type: yum
          packages: [ wget ]
  ```

You can now include ADD and COPY instructions in the config file:

  ```yaml
  platforms:
    - type: docker
      image: bash:latest
      setup:
        - type: docker
          add: [ "http://foo.bar ." ]
  ```

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

# Viash 0.5.6 (2022-02-03): Forbidden Bash flags have been renamed

* Viash can now be installed without Docker needing to be installed on your system. You do need `unzip` and `wget` to complete the installation.
* The Docker related messages are more user friendly now.

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
  

# Viash 0.5.5 (2021-12-17): Resources dir no longer added to PATH automatically and minor changes

The resources directory is no longer added to the PATH variable by default. You can re-enable this behaviour by setting add_resources_to_path to `true` in the functionality part of the config file.  
Here's a snippet of a config file to illustrate this:

  ```yaml
  functionality:
    name: example_component
    description: Serve as a simple example.
    add_resources_to_path: true
    ...
  ```

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

# Viash 0.5.4 (2021-09-20): Added cache directive to specify the typing of caching to be performed for the Nextflow platform

A cache type can now be specified in the config file for the Nextflow platform. Previously this was hardcoded to be `deep`, but the default caching method is now `default`.  
To use deep caching again, add this to your config file:

  ```yaml
  cache: deep
  ```

## BREAKING CHANGES

* `NextflowPlatform`: The default caching mechanism is now what Nextflow uses as default. In order to replicate earlier
  caching, `cache: deep` should be specified in the Viash config file.

## NEW FEATURES

* `NextflowPlatform`: Added `cache` directive to specify the typing of caching to be performed.

# Viash 0.5.3 (2021-09-02): New meta data list for scripts, VIASH_TEMP environment variable for Nextflow, fixed output formatting with separate outputs

This release provides more information to scripts with the new `meta` list. This list contains two values for now:

  - `meta["resources_dir"]`: Path to the directory containing the resources
  - `meta["functionality_name"]`: Name of the component

A new environment variable is now available for export when working with the Nextflow platform: `VIASH_TEMP`.

## Resources directory

All resources defined in the config file are copied over to a temporary location right before a Viash component is executed. This location is can now be easily accessed in your scripts, allowing you to modify and copy the files as needed.  
Here are some examples in different scripting languages on how to access the meta data, it works similarly to the `par` list:

Bash:  

  ```bash
  echo $meta_resources_dir 
  ```

Python:  

  ```python
  print(meta["resources_dir"])
  ```

R:

  ```r
  cat(meta$resources_dir)
  ```

## Functionality name

The name of the component can now be accessed in the same way as the resources directory. This allows you to print the name of the component out to a console window for example.
Here's how to access this data in different scripting languages:

Bash:

  ```bash
  echo $meta_functionality_name
  ```

Python:  

  ```python
  print(meta["functionality_name"])
  ```

R:

  ```r
  cat(meta$functionality_name)
  ```

## NEW FEATURES

* Similar to `par`, each script now also has a `meta` list. `meta` contains meta information about the component
  or the execution thereof. It currently has the following fields:
  - `meta["resources_dir"]`: Path to the directory containing the resources
  - `meta["functionality_name"]`: Name of the component

* `NextflowPlatform`: Export `VIASH_TEMP` environment variable. 

## BUG FIXES

* `NextflowPlatform`: Fix output formatting when `separate_multiple_outputs` is `false`.

# Viash 0.5.2 (2021-08-13): More settings for Docker and Nextflow platform, and a bug fixes for components with resources

This is a small release containing two small features and a bug fix.
The new `run_args` field allows you to add [docker run](https://docs.docker.com/engine/reference/commandline/run/) arguments to the [Docker platform](/reference/config/platforms/docker/#) section of a [config file](/reference/config/index.html). For example:

  ```yaml
  platforms:
    - type: docker
      image: bash:4.0
      run_args: "--expose 127.0.0.1:80:8080/tcp --env MY_ENV_VAR=foo"
  ```

There's also a new field for the [Nextflow platform](/reference/config/platforms/nextflow/#): `separate_multiple_outputs`. By default, this is set to `true` and separates the outputs generated by a Nextflow component with multiple outputs as separate events on the channel. You can now choose to disable this behaviour:

  ```yaml
  platforms:
    - type: nextflow
      publish: true
      separate_multiple_outputs: false
  ```

## MINOR CHANGES

* `DockerPlatform`: Added `run_args` field to allow setting `docker run` arguments.

* `NextflowPlatform`: Added argument `separate_multiple_outputs` to allow not separating the outputs generated by a 
  component with multiple outputs as separate events on the channel.

## BUG FIX

* `IO`: Allow overwriting directory resources upon rebuild.

# Viash 0.5.1 (2021-07-14): Viash 0.5.1 adds support for C# scripts and fixes a few bugs

## C# script support

We've added C# scripts (.csx) as a supported language using **dotnet-script**.  
To run C# scripts natively, you'll need to install .NET Core and execute the following command in a terminal:

  ```bash
  dotnet tool install -g dotnet-script
  ```

You can now run C# scripts like this:

  ```bash
  dotnet script hello_viash.csx
  ```

To use C# scripts as components, use the new `csharp_script` type in the functionality section of your config file:

  ```yaml
    resources:
    - type: csharp_script
      path: script.csx
  ```

Here's an example of a simple C# script with Viash in mind:

  ```csharp
  // VIASH START
  var par = new {
    input = "Hello World",
    name = "Mike"
  };
  // VIASH END

  System.Console.WriteLine(input + ", " + name + "!");
  ```

The language-specific guide for creating C# script components will be added in the near future.

## Bug fixes

First off, these special characters  can now be used in the description, usage, default and example fields of components:

- "
- \`
- \\
- \n
- $

Nextflow output files with the same extension won't overwrite each other any more, like it was the case for arguments like this:

  ```yaml
  functionality:
    name: bar
    arguments:
      - name: "--input"
        type: file
        example: input.txt
      - name: "--output1"
        type: file
        direction: output
        required: true
        example: output.txt
      - name: "--output2"
        type: file
        direction: output
        required: true
        example: optional.txt
  ```

In this case, the two output files would have been identical in the past.
___

## NEW FEATURES

* `CSharpScript`: Added support for C# scripts (`type: "csharp_script"`) to viash.

## MINOR CHANGES

* `NextflowPlatform`: Added `directive_cpus`, `directive_max_forks`, `directive_memory` and `directive_time` parameters.

## BUG FIXES

* `BashWrapper`: Refactor escaping descriptions, usages, defaults, and examples (#34).

* `NextflowPlatform`: Refactor escaping descriptions, usages, defaults and examples (#75).

* `NextflowPlatform`: Add argument to output path to avoid naming conflicts for components with multiple output files (#76).

* `NextflowPlatform`, `renderCLI()`: Only add flag to rendered command when boolean_true is actually true (#78).

* `DockerPlatform`: Only chown when output file exists.

## TESTING

* `viash build`: Capture stdout messages when errors are expected, so that they don't clutter the expected output.

* `viash build`: Check `--help` description output on the whole text instead of per letter or word basis.

* `TestingAllComponentsSuite`: Only testing bash natively, because other dependencies might not be available.

# Viash 0.5.0 (2021-08-16): Improvements to running Docker executables, and Nextflow platform argument changes

Here are the most important changes:

* **Improvements to Docker backend**: In the past, you needed to perform `--setup` on your Docker-based components and executables in order for the image to be built before you could run the component or executable. Now you can simply run your component or executable and Viash will do the image building automatically by default if it detects an image isn't present yet. This behaviour can be changed by using a Docker setup strategy. For example:

  ```bash
  viash build config.vsh.yaml -p docker --setup alwayscachedbuild
  ```

* **Nextflow gets some argument changes**: Arguments for the Nextflow platform now have optional `required` and `default` values, just like their native and Docker counterparts. For example:

  ```yaml
    arguments:
      - name: --name
        type: string
        description: Input name
        required: true
      - name: --repeat
        type: integer
        description: Times to repeat the name
        default: 100
  ```

  Take a look at the Functionality page for more information on arguments and their properties.  
  As long as you use long-option arguments (e.g. `--my-option`) in the config file for required arguments, the way of specifying argument values for the Nextflow platform is identical to the Docker platform. You still access non-required arguments via this syntax: `--<component_name>__<argument_name> <value>`. For example:

  ```bash
  my_component -- --my_component__input Hello!
  ```

* **Verbosity levels for viash run**: Executables now have 8 levels of verbosity

  0. emergency
  1. alert
  2. critical
  3. error
  4. warning
  5. notice
  6. info
  7. debug

  The default verbosity level is **notice**.
  You can pass the `-v` or `--verbose` option to bump up the verbosity by one level. By passing `-vv` the verbosity goes up by two levels. You can manually set the verbosity by using the `--verbosity <int_level>` option. For example, if you wanted to only show errors or worse:

  ```bash
  viash run config.vsh.yaml -- --verbosity 3
  ```

## BREAKING CHANGES

* `DockerPlatform`: A Docker setup will be performed by default. Default strategy has been changed to `ifneedbepullelsecachedbuild` (#57).
  `---setup` strategy has been removed and `---docker_setup_strategy` has been renamed to `---setup`.
  This change allows running a component for the first time. During first time setup, the Docker container will be pulled or built automatically. 

* `NativePlatform`: Deprecated the native setup field.

## MAJOR CHANGES

* `NXF`: This version changes the handling logic for arguments. An argument can be either `required` or not and can have a `default: ...` value or not. Checks are implemented to verify that required arguments are effectively provided _during_ pipeline running.

* `NXF`: If one sticks to long-option argments in the viash config, for all arguments that are _required_, the way of specifying the arguments on the CLI is identical for the Docker and Nextflow platforms. Non-required arguments can still be accessed from CLI using `--<component_name>__<argument_name> ...`.

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

# Viash 0.4.0.1 (2021-05-12): Three small bug fixes.

## BUG FIX

* `NXF`: Return original_params instead of updated params for now.

* `NXF`: Reinstate function_type: asis in line with the refactored module generation code

* `viash ns test`: print header when `--tsv foo.tsv --append true` but foo.tsv doesn't exist yet. Fixes #45.

# Viash 0.4.0 (2021-04-14): Config mod DSL and renames to viash ns arguments

The viash ns command's --namespace argument has been renamed to --query_namespace, introduction of custom DSL for overriding config properties at runtime.

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

* `NXF`: Allow for multiple output files/directories from a module while keeping compatibility for single output. Please [refer to the docs](/reference/config/platforms/nextflow/#multiple-outputs).

* `NXF`: Allow for zero input files by means of passing an empty list `[]` in the triplet

* `NXF`: Remove requirement for `function_type: todir`

* `NXF`: It is now possible to not only specify `label: ...` for a nextflow platform but also `labels: [ ...]`.
  
## BUG FIXES

* Allow quotes in functionality descriptions.

* `NXF`: Providing a `default: ...` value for output file arguments is no longer necessary.

# Viash 0.3.2 (2021-02-04): Don't auto-generate viash.yaml and add beta unit testing in Nextflow

The viash build command doesn't generate a viash.yaml automatically anymore, added beta functionality for running tests in Nextflow.

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

# Viash 0.3.1 (2021-01-26): Add fields for specifying authors and the Docker registry

Add authors field to config, added registry fields to Docker platform config.

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

# Viash 0.3.0 (2020-11-24): Combine functionality and platform into one config, remove temporary files

`config.vsh.yaml` is the new standard format, temporary files are removed when using run and test commands.

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

# Viash 0.1.0 (2020-05-14): Changes to functionality and the native/docker platforms

## Changes to functionality.yaml

* ftype has been renamed to function_type. The value for this field is also being checked.
* platform has been removed.
* Instead, the first resource listed is expected to have `type: r_script`, `type: bash_script`, `type: python_script`, or `type: executable`. The other resources are expected to have `type: file` by default, and are left untouched by Viash.
* in the arguments, field `flagValue` has been removed. Instead, use `type: boolean_true` and `type: boolean_false` to achieve the same effect.

## Changes to platform_(docker/native).yaml

* The `r: packages:` field has been renamed to `r: cran:`.

## MAJOR CHANGES

* Refactoring of the Functionality class as discussed in VIP1 (#1). This has resulted in a lot of internal changes, but the changes with regard to the yaml definitions are relatively minor. See the section below for more info.

## MINOR CHANGES

* Updated the functionality.yamls under `atoms/` and `src/test/` to reflect these aforementioned changes.
* Allow for bioconductor and other repositories in the R environment.
* Add support for pip versioning syntax.

## BUG FIXES

* Do not quote passthrough flags.
* Allow for spaces inside of Docker volume paths.

## DOCUMENTATION

* Updated the README.md.
* Provide some small examples at `doc/examples`.

# Viash 0.0.1 (2020-05-05): Initial release

* Initial proof of concept.
