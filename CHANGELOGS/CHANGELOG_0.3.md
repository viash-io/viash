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
