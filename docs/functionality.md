Description of the functionality.yaml format
================

  - [name \[string\]](#name-string)
  - [description \[string\]](#description-string)
  - [arguments \[list\]](#arguments-list)
      - [type: string](#type-string)
      - [type: file](#type-file)
      - [type: integer](#type-integer)
      - [type: double](#type-double)
      - [type: boolean](#type-boolean)
      - [type:
        boolean\_true/boolean\_false](#type-boolean_trueboolean_false)
  - [resources \[list\]](#resources-list)
      - [type: file](#type-file-1)
      - [type: r\_script](#type-r_script)
      - [type: python\_script](#type-python_script)
      - [type: bash\_script](#type-bash_script)
      - [type: executable](#type-executable)
  - [tests \[list\]](#tests-list)
  - [function\_type \[string\]](#function_type-string)

The functionality yaml is a meta file which describes the behavior of a
script in terms of input/output/parameters. By specifying a few
restrictions (e.g. mandatory arguments) and adding some descriptions,
viash can automatically generate a command-line interface.

An example of such a functionality yaml can be found below, each part of
which is explained in more depth in the following sections. For more
(extensive) examples, see [examples](examples).

``` yaml
name: exe
description: |
  This component performs function Y and Z.
  It is possible to make this a multiline string.
function_type: transform
arguments:
- name: --input                           
  type: file
  alternatives: [-i]
  description: Input file(s)
  default: input.txt
  must_exist: true
  required: false
  multiple: true
  multiple_sep: ","
resources:
- type: r_script
  path: script.R
tests:
- type: r_script
  path: tests/unit_test.R
```

## name \[string\]

Name of the component described.

Example:

``` yaml
name: exe
```

## description \[string\]

A description of the component. This will be displayed with `--help` and
in the README (if created).

Example:

``` yaml
description: |
  This component performs function Y and Z.
  It is possible to make this a multiline string.
```

## arguments \[list\]

A list of arguments for this component. For each argument, a type and a
name must be specified. Depending on the type of argument, different
properties can be set. Common properties for all argument types are the
following.

  - `type:
    string/file/integer/double/boolean/boolean_true/boolean_false`, the
    type of argument determining to what object type the value will be
    cast in the downstream scripts.
  - `name: --foo`, the name of the argument. Can also be `-foo` or
    `foo`. The number of dashes determines how values can be passed:
      - with `--foo`: long option, e.g. `exe --foo=bar` or `exe --foo
        bar`
      - with `-foo`: short option, e.g. `exe -foo bar`
      - with `foo`: argument, e.g. `exe bar`
  - `alternatives: [-f]`, list of alternative names. Typically only used
    to provide a short alternative option.
  - `description: Description of foo`, a description of the argument.
    Multiline descriptions are supported.
  - `default: bar`, the default value when no argument value is
    provided. Not allowed when `required: true`.
  - `required: true/false`, whether the argument is required. If true
    and the functionality is executed, an error will be produced if no
    value is provided. Default = false.
  - `multiple: true/false`, whether to treat the argument value as an
    array or not. Arrays can be passed using the delimiter `--foo=1:2:3`
    or by providing the same argument multiple times `--foo 1 --foo 2`.
    Default = false.
  - `multiple_sep: ":"`, the delimiter for providing multiple values.
    Default = “:”.

Example:

``` yaml
- name: --foo                           
  type: file
  alternatives: [-f]
  description: Description of foo
  default: "/foo/bar"
  must_exist: true
  required: false
  multiple: true
  multiple_sep: ","
```

#### type: string

The value passed through an argument of this type is converted to an
‘str’ object in Python, and to a ‘character’ object in R.

#### type: file

The resulting value is still an ‘str’ in Python and a ‘character’ in R.
However, when using a Docker platform, the value will automatically be
substituted with the path of the mounted directory inside the container
(see [platform\_docker.md](platform_docker.md). Additional property
values: \* `must_exist: true/false`, denotes whether the file or folder
should exist at the start of the execution. \* `direction:
input/output/log`, specifies whether the file is an input, an output, or
a log file.

#### type: integer

The resulting value is an ‘int’ in Python and an ‘integer’ in R.

#### type: double

The resulting value is a ‘float’ in Python and an ‘double’ in R.

#### type: boolean

The resulting value is a ‘bool’ in Python and a ‘logical’ in R.

#### type: boolean\_true/boolean\_false

Arguments of this type can only be used by providing a flag `--foo` or
not. The resulting value is a ‘bool’ in Python and a ‘logical’ in R.
These properties cannot be altered: required is false, default is
undefined, multiple is false.

## resources \[list\]

The first resource should be a script (with type
r\_script/python\_script/bash\_script) or an executable, which is what
will be executed when the functionality is run. A script is a file for
which the code should be provided, whereas executable is a pre-existing
executable which should be available on the PATH of the host system.

When using viash to export the functionality, the first resource will be
renamed to the name of the functionality, and will be made executable.
Additional resources will be copied to the same directory.

Common properties:

  - `type: file/r_script/python_script/bash_script/executable`, the type
    of resource.
  - `name: filename`, the resulting name of the resource.
  - `path: path/to/file`, the path of the input file. Can be a relative
    or an absolute path, or a URI.
  - `text: ...multiline text...`, the raw content of the input file.
    Exactly one of `path` or `text` must be defined, the other
    undefined.
  - `is_executable: true/false`, whether the resulting file is made
    executable.

#### type: file

A simple file which will be copied when the functionality is exported.
The first resource cannot be of this type. If the type of a resource is
not given, it is assumed to be of type `file`.

#### type: r\_script

An R script. Must contain a code block starting with `"VIASH START"` and
ending with `"VIASH END"`, which will be replaced if at run-time if the
script is the first resource. See
[wrapping\_an\_r\_script.md](wrapping_an_r_script.md) for more
information.

#### type: python\_script

A python script. Must contain a code block starting with `"VIASH START"`
and ending with `"VIASH END"`, which will be replaced if at run-time if
the script is the first resource. See
[wrapping\_a\_python\_script.md](wrapping_a_python_script.md) for more
information.

#### type: bash\_script

A bash script. Must contain a code block starting with `"VIASH START"`
and ending with `"VIASH END"`, which will be replaced if at run-time if
the script is the first resource. See
[wrapping\_a\_bash\_script.md](wrapping_a_bash_script.md) for more
information.

#### type: executable

An executable which is already available on the platform. Since the
executable is assumed to handle the command-line interface by itself,
there should be a one-to-one mapping of the arguments described in the
functionality and the arguments that the executable ‘understands’. See
[wrapping\_an\_executable.md](wrapping_an_executable.md) for more
information.

## tests \[list\]

Similar to resources, the test resources will only be used when using
the `viash test` command. Each r\_script/python\_script/bash\_script is
assumed to contain unit tests which tests whether the functionality
exported by viash functions as intended and exits with a code \> 1 when
unexpected behavior occurs. Other files are assumed to be files used
during a test.

## function\_type \[string\]

The function\_type is used in Nextflow to describe the type of function
the functionality provides. The function type affects two aspects: how
many files can be used as input and output, and how should the output
files be named (based on the input).

See [platform\_nextflow.md](platform_nextflow.md) for an explanation
between the difference between function types.
