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
