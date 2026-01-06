# Viash 0.9.5 (2025-10-06): Hotfix for internal CI usage

This is a hotfix release to support internal CI usage and testing.
There is no functionality change for regular users.

## MINOR CHANGES

* `Vsh repository`: Add support for overriding host resolving (PR #854). This is meant for internal usage and testing and will be replaced in the near future.

## BUG FIXES

* `Dependencies`: Fix an issue where deeply nested dependencies are not resolvable if they require a local dependency (PR #838).
  This solves a build issue, some issues still remain with running components with such dependencies.

# Viash 0.9.4 (2025-04-24): Hotfix for Nextflow edge

This is a hotfix release for the changes in Nextflow edge's handling of double arguments.

## BUG FIXES

* `NextflowRunner`: Automatically convert integers to doubles when argument type is `double` (port of PR #823, PR #824).

# Viash 0.9.3 (2025-03-31): Support upcoming version of Nextflow

The upcoming release of Nextflow introduces a new class for loading scripts and renamed the old class.
This release supports this change by using reflection to detect the available class.

## BUG FIXES

* `NextflowRunner`: ScriptParser was renamed to ScriptLoaderV1 in Nextflow 25.02.0-edge (PR #812). This fix uses reflection
  to detect whether ScriptParser exists -- if not the ScriptLoaderFactory is used instead.

* `NextflowRunner`: Make sure scripts are generated with the right extension (PR #815).

# Viash 0.9.2 (2025-03-04): Bug fix release

This release fixes an edge case where output arguments for subworkflows were malformed.

## MINOR CHANGES

* CI: Added a CI for creating a release (PR #805).

## BUG FIXES

* `NextflowRunner`: Fix issue where output arguments for subworkflows were being returned as nested arrays instead of simple arrays (PR #798, PR #800).

# Viash 0.9.1 (2024-12-16): Enhanced nextflow support and Scala 3 update

Workflows can now publish results asynchronously by emitting multiple output channels. These results will then be merged into a published output behind the screens.
Dependencies will use the new dedicated git url instead of the top level domain name.

## NEW FEATURES

* `Nextflow` runner: allow emitting multiple output channels (PR #736).

* `Scope`: Add a `scope` field to the config (PR #782). This allows tuning how the components is built for release.

## MINOR CHANGES

* `viash-hub`: Change the url for viash-hub Git access to packages.viash-hub.com (PR #774).

* `RRequirements`: Allow single quotes to be used again in the `.script` field (PR #771).

* `scala`: Update Scala to Scala 3 (PR #759).
  For most of the code, this was a minor update, so no breaking changes are expected.
  The biggest change is how the exporting of the schema is done, but this has no impact on the user.
  However, switching to Scala 3 allows for additional features and improvements in the future.

* `--help`: Component `--help` messages will now display what built in `---` options are available (PR #784).

## BUG FIXES

* `config build`: Fix a bug where a missing main script would cause a stack trace instead of a proper error message (PR #776).
  The error message showed the path of the missing resource but it was easy to miss given the stack trace, besides it shouldn't have been a stack trace anyway.
  
* `RRequirements`: Treat warnings as errors when installing R dependencies in Docker engines (PR #771).

* `Nextflow` runner: fix false-positive error when output argument arguments `required: true` 
  are incorrectly flagged as missing input arguments (PR #778).

# Viash 0.9.0 (2024-09-03): Restructure platforms into runners and engines

This release restructures the introduces changes to the Viash config:
- The `platforms` field is split into `runners` and `engines`
- The `.functionality` layer has been removed from the config and all fields have been moved to the top layer

Changes are made to sanitize the built config output and include additional relevant meta data.
The default `multiple_sep` has been changed from `:` to `;` to avoid conflicts with paths like `s3://foo/bar`.

Implemented a proper way of caching dependency repositories. The cache is stored under `~/.viash/repositories`.

## BREAKING CHANGES

* `runners` and `engines`: The usage of `platforms` is deprecated and instead these are split into `runners` and `engines` (PR #510). 
  The `platforms` field is still supported but will be removed in a future release.
  In brief, the `native platform` became a `native engine` and `docker platform` became a `docker engine`.
  Additionally, the `native platform` and `docker platform` became a `executable runner`, `nextflow platform` became a `nextflow runner`.
  The fields of `docker platform` is split between `docker engine` and `docker runner`: `port`, `workdir`, `setup_strategy`, and `run_args` (set to `docker_run_args`) are captured by the `runner` as they define how the component is run. The other fields are captured by the `engine` as they define the environment in which the component is run. One exception is `chown` which is rarely set to false and is now always enabled.

* `arguments`: Merge arguments into argument_groups during a json decode prepare step (PR #574). The `--parse_argument_groups` option from `ns list` and `config view` is deprecated as it is now always enabled.

* `arguments`: Change default `multiple_sep` from `:` to `;` to avoid conflicts with paths like `s3://foo/bar` (PR #645).
  The previous behaviour of using `multiple_sep: ":"` can be achieved by adding a config mod to the `_viash.yaml`:
  ```yaml
  config_mods: |
    .functionality.argument_groups[true].arguments[.multiple == true].multiple_sep := ":"
  ```

* `functionality`: Remove the `functionality` layer from the config and move all fields to the top layer (PR #649).

* `computational requirements`: Use 1000-base units instead of 1024-base units for memory (PR #686). Additionally, the memory units `kib`, `mib`, `gib`, `tib`, and `pib` are added to support 1024-base definitions.

* `NextflowEngine`: Swap the order of execution of `runIf` and `filter` when calling `.run()`. This means that `runIf` is now executed before `filter` (PR #660).

## NEW FUNCTIONALITY

* `export json_schema`: Add a `--strict` option to output a subset of the schema representing the internal structure of the Viash config (PR #564).

* `config view` and `ns list`: Do not output internal functionality fields (#564). Additionally, add a validation that no internal fields are present when reading a Viash config file.

* `project config`: Add fields in the project config to specify default values for component config fields (PR #612). This allows for a more DRY approach to defining the same values for multiple components.

* `dependencies`: GitHub and ViashHub repositories now get properly cached (PR #699).
  The cache is stored in the `~/.viash/repositories` directory using sparse-checkout to only fetch the necessary files.
  During a build, the cache is checked for the repository and if it is found and still up-to-date, the repository is not cloned again and instead the cache is copied to a temporary folder where the files are checked out from the sparse-checkout.

* `ExecutableRunner`: Add a `---docker_image_id` flag to view the Docker image ID of a built executable (PR #741).

* `viash ns query`: Add a query filter that allows selecting a single component by its path in a namespace environment (PR #744).

* `config schema`: Add `label` & `summary` fields for Config, PackageConfig, argument groups, and all argument types (PR #743).

* `NextflowEngine`: Added `runIf` functionality to `runEach` (PR #660).

## MINOR CHANGES

* `testbenches`: Add testbenches for local dependencies (PR #565).

* `testbenches`: Refactor testbenches helper functions to uniformize them (PR #565).

* `logging`: Preserve log order of StdOut and StdErr messages during reading configs in namespaces (PR #571).

* `Java 21 support`: Update Scala to 2.13.12 and update dependencies (PR #602).

* `project config`: Output the project config under the default name `ProjectConfig` instead of `Project` during schema export (PR #631). This is now important as the project config is now part of the component config. Previously this was overridden as the class name was `ViashProject` which was less descriptive.

* `package config`: Renamed `project config` to `package config` (PR #636). Now that we start using the config more, we came to the conclusion that "package" was better suited than "project".

* `ns exec`: Added an extra field `{name}` to replace `{functionality-name}` (PR #649). No immediate removal of the old field is planned, but it is deprecated.

* `BashWrapper`: Added meta-data field `meta_name` as a replacement for `meta_functionality_name` (PR #649). No immediate removal of the old field is planned, but it is deprecated.

* `error message`: Improve the error message when using an invalid field in the config (#PR #662). The error message now includes the field names that are not valid if that happens to be the case or otherwise a more general error message.

* `config mods`: Improve the displayed error message when a config mod could not be applied because of an invalid path (PR #672).

* `docker_engine`: Deprecate `registry`, `organization` and `tag` fields in the `docker_engine` (PR #712). Currently these are hardly ever used and instead the `image` field is used to specify the full image name.

* `docker_engine`: Add `target_package` field to the `docker_engine` (PR #712). This field, together with the `target_organization` is used to specify the full built container image name. The fields use proper fallback for the values set in the component config and package config.

* `organization`: Remove the `organization` field from the component config (PR #712). The value is now directly used by the `docker_engine` as a fallback from the `target_organization` field.

* `ExecutableRunner`: Add parameter `docker_automount_prefix` to allow for a custom prefix for automounted folders (PR #739).

* `ExecutableRunner`: Make Docker runtime arguments configurable via the `---docker_run_args` argument (PR #740).

* `export json_schema`: Add `arguments` field to the `Config` schema (PR #755). Only for the non-strict version, the strict version of the viash config has these values merged into `argument_groups`.

* `scala`: Update Scala to 2.13.14 (PR #764).

* `NextflowEngine`: Also parse `${id}` and `${key}` aside from `$id` and `$key` as identifier placeholders for filenames (PR #756).

## BUG FIXES

* `__merge__`: Handle invalid yaml during merging (PR #570). There was not enough error handling during this operation. Switched to the more advanced `Convert.textToJson` helper method.

* `config`: Anonymize paths in the config when outputting the config (PR #625).

* `schema`: Don't require undocumented fields to set default values and add the `links` and `reference` fields to functionality as they were not meant only to be in the project config (PR #636).

* `export json_schema`: Fix minor inconsistencies and make the strict schema stricter by adapting to what Viash will effectively return (PR #666).

* `deprecation & removal warning`: Improve the displayed warning where a deprecated or removed field could display a double '.' when it field was located at the root level (PR #671).

* `resource path`: Don't finalize the `path` field of a resource until it's written as part of building a component (PR #668).

* `requirements`: Improve the error message when a Python or R requirement uses a single quote in the `.script` field (PR #675).

* `viash test`: Fix Docker id between build and test components not being consistent when using a custom Docker registry (PR #679).

* `MainNSBuildNativeSuite`: Capture the error message when reading the configs so we can capture the expected warning message (PR #688).
  While almost all tests were already cleanly capturing their expected warning/error messages, this one was still remaining, resulting in warnings being shown in the output.

* `runners & engines`: When applying a filter on empty runners or engines, the fallback default `native engine` and `executable runner` respectively are set before applying the filter (PR #691).

* `dependencies`: Fix resolving of dependencies of dependencies (PR #701). The stricter build config was now lacking the necessary information to resolve dependencies of dependencies.
  We added it back as `.build_info.dependencies` in a more structured, anonymized way.

* `dependencies`: Fix the `name` field of repositories possibly being outputted in the build config (PR #703).

* `symlinks`: Allow following of symlinks when finding configs (PR #704). This improves symlink functionality for `viash ns ...` and dependency resolving.

* `build_info`: Correctly set the `.build_info.executable` to `main.nf` when building a component with a Nextflow runner (PR #720).

* `vsh organization`: ViashHub repositories now use `vsh` as the default organization (PR #718).
  Instead of having to specify `repo: vsh/repo_name`, you can now just specify `repo: repo_name`, which is now also the prefered way.

* `testbenches`: Add a testbench to verify dependencies in dependencies from scratch (PR #721).
  The components are built from scratch and the dependencies are resolved from the local repositories.

* `docker_engine`: Fix a bug in how the namespace separator is handled (PR #722).

* `platforms`: Re-introduce the `--platform` and `--apply_platform` arguments to improve backwards compatibility (PR #725).
  When the argument is used, a deprecation warning message is printed on stderr.
  Cannot be used together with `--engine` or `--runner` and/or `--apply_engine` or `--apply_runner`.

* `nextflow_runner`: Fix refactoring error in the `findStates()` helper function (PR #733).

* `viash ns exec`: Fix "relative fields" outputting absolute paths (PR# 737). Additionally, improve path resolution when using the `--src` argument.

* `viash ns`: Fix viash tripping over its toes when it encounters multiple failed configs (PR #761). A dummy config was used as a placeholder, but it always used the name `failed`, so duplicate config names were generated, which we check for nowadays.

* `bashwrapper`: Fix an issue where running `viash test` which builds the test docker container would ignore test failures but subsequential runs would work correctly (PR #754).

* `NextflowEngine`: Fix escaping of odd filename containing special characters (PR #756). Filenames containing a `$` character caused Bash to try to interpret it as a variable.

* `json schema`: Fix repositories types with name incorrectly adding `withname` as type (PR #768).

* `json schema`: Change the '$schema' field to 'http://' instead of 'https://' (PR #768). (Some?) Json validators use this value as a token and not as a URL.

* `viash test`: Fix an issue where the tests would not copy package config settings to determine the docker image name (PR #767).
