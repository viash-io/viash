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
