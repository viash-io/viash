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
