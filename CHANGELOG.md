# viash 0.4.0 (2021-04-08)

## BREAKING CHANGES

* `viash ns`: Argument `--namespace` has been renamed to `--query_namespace`.

* `viash ns`: Argument `--namespace` does not implicitly change the namespace of the functionality anymore.
  You can use the command DSL to reproduce this effect; for example: `-c '.functionality.namespace := "foo"'`.

* [NXF] When running a module on its own all inputs with `type: file` should be referenced the moment the pipeline is started. Previous behavior was to only us `--input` and map that to the first argument of `type: file`. For example: say `--input` and `--reference` both are input file arguments, the following should be used:

    ```
    nextflow run main.nf --input ... --reference
    ```

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

* Docker image parsing for `docker` and `nextflow` platforms are aligned. It is now also possible to choose between specifying, e.g. `image: ubuntu:latest` and `{ image: ubuntu, tag: latest }`.

* [NXF] The implicitly generated names for output files/directories have been improved leading to less clashes.

* [NXF] Allow for multiple output files/directories from a module while keeping compatibility for single output. Please [refer to the docs](http://www.data-intuitive.com/viash_docs/config/platform-nextflow/#multiple-outputs).

* [NXF] Allow for zero input files by means of passing an empty list `[]` in the triplet

* [NXF] Remove requirement for `function_type: todir`

* [NXF] It is now possible to not only specify `label: ...` for a nextflow platform but also `labels: [ ...]`.

## BUG FIXES

* Allow quotes in functionality descriptions.

* [NXF] Providing a `default: ...` value for output file arguments is no longer necessary.

## UNDER THE HOOD

* [NXF] Module generation has been refactored thoroughly. 


# viash 0.3.2 (2021-02-04)

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

# viash 0.3.1 (2021-01-26)

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

* Docker Platform: Allow specifying the registry with `target_registry`. Example:

```yaml
- type: docker
  image: bash:4.0
  target_registry: foo.io
  target_image: bar
  target_tag: 0.1
```

* Docker Platform: `version` is now a synonym for `target_tag`.
  If both `version` and `target_tag` are not defined, `functionality.version` will
  be used instead.
  
* Docker Platform: Can change the Docker Setup Strategy by specifying
  - in the yaml: `setup_strategy: xxx`
  - on command-line: `---docker_setup_strategy xxx` or `---dss xxx`
  
  Allowed values for the setup strategy are:
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


# viash 0.3.0 (2020-11-24)

## BREAKING CHANGES

* File format `functionality.yaml` is no longer supported. Use `config.vsh.yaml` or `script.vsh.R/py/...` instead.

* `viash run` and `viash test`: By default, temporary files are removed when the execution succeeded, otherwise they are kept. 
  This behaviour can be overridden by specifying `--keep true` to always keep the temporary files, and `--keep false` to always remove them.

* `NXF`: `function_type: todir` now returns the output directory on the `Channel` rather than its contents.

## NEW FEATURES

* Added `viash ns test`: Run all tests in a particular namespace. For each test, the exit code and duration is reported. Results can be written to a tsv file.
* Added support for JavaScript scripts.
* Added support for Scala scripts.
* [NXF] publishing has a few more options:
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


# viash 0.2.2 (2020-09-22)

* MINOR CHANGE: Allow generating placeholder without VIASH START/VIASH END blocks.
* BUG FIX `viash ns build`: Some platforms would sometimes not be detected.
* BUG FIX `viash run`: Avoid error when no arguments need to be chowned.

# viash 0.2.1 (2020-09-11)

* NEW FEATURE `NXF`: Data references in Map form can now have values being lists. In other words, we can have multiple options which have one or more values.
* NEW FEATURE `viash ns build`: Added --parallel and --setup flag.
* NEW FEATURE `viash build`: Added --setup flag.
* NEW FEATURE: Allow changing the order of setup commands using the `setup:` variable.
* NEW (HIDDEN) FEATURE: Do not escape `${VIASH_...}` elements in default values and descriptions!
* MINOR CHANGE: Remove `---chown` flag, move to `platform.docker.chown`; is set to true by default.
* MINOR CHANGE: Perform chown during both run and test using a Docker platform.
* BUG FIX: Issue trying to parse positional arguments even when none is provided.

# viash 0.2.0 (2020-09-01)

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
* [NXF] Join operations are now fully supported by means of `multiple`.
* [NXF] Modules that perform joins can take either arrays (multiple input files or the same type to be joined) or hashes (multiple input files passed using different options on the CLI). Please refer to the docs for more info.

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
* [NXF] _Modules_ are now stored under `target/nextflow` by default

## BUG FIXES
* NXF: Correctly escape path variable when running NXF command.
* NXF: Surround parameters with quotes when running NXF command.

## INTERNAL CHANGES
* Move CLI from inner script to outer script.
* Renamed Target to Platform
* Renamed Environment to Requirements

# viash 0.1.0 (2020-05-14)
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

# viash 0.0.1 (2020-05-05)
* Initial proof of concept.
