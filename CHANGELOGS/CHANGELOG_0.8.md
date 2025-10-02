# Viash 0.8.8 (2025-04-24): Hotfix for Nextflow edge

This is a hotfix release for the changes in Nextflow edge's handling of double arguments.

## BUG FIXES

* `NextflowRunner`: Automatically convert integers to doubles when argument type is `double` (PR #823).

# Viash 0.8.7 (2025-04-01): Backport support upcoming version of Nextflow

The upcoming release of Nextflow introduces a new class for loading scripts and renamed the old class.
This release supports this change by using reflection to detect the available class.

## BUG FIXES

* `viash build`: Fix error handling of non-generic errors in the build process or while pushing docker containers (PR #696).

* `NextflowRunner`: ScriptParser was renamed to ScriptLoaderV1 in Nextflow 25.02.0-edge (PR #817). Backport from Viash 0.9.3 (PR #812).
  This fix uses reflection to detect whether ScriptParser exists -- if not the ScriptLoaderFactory is used instead.

* `NextflowRunner`: Backport path handling for `meta.resources_dir` for when symlinks are used (PR #817).

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
