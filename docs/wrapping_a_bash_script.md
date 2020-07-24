Wrapping a Bash script with viash
================

This vignette demonstrates how to wrap a bash script with viash.

## Demonstration

This component is the simplest of examples. Its files are located at
[examples/hello\_world](examples/hello_world).

By running the component, it will output “Hello world\!”, followed by
any other inputs provided to it.

``` bash
cd examples/hello_world
viash run -f functionality.yaml -- I am viash!
```

    ## Hello world! I am viash!

It can also be run with a Docker backend, by providing viash with a
platform
yaml.

``` bash
viash run -f functionality.yaml -p platform_docker.yaml -- General Kenobi. --greeter="Hello there."
```

    ## Hello there. General Kenobi.

Now that we know what the component does, we can export the
functionality as an executable.

``` bash
viash export -f functionality.yaml -p platform_docker.yaml -o output
output/hello_world And now, as an executable.
```

    ## Hello world! And now, as an executable.

By running the command with a `--help` flag, more information about the
component is provided.

``` bash
output/hello_world --help
```

    ## A very simple 'Hello world' component.
    ## 
    ## Options:
    ##     string1 string2 ...
    ##         type: string, multiple values allowed
    ## 
    ##     --greeter=string
    ##         type: string, default: Hello world!

To verify that the component works, use `viash test`. This can be run
both with or without the Docker backend.

``` bash
viash test -f functionality.yaml -p platform_docker.yaml
```

    ## 
    ## SUCCESS! All 2 out of 2 test scripts succeeded!
    ## Cleaning up temporary files

## Development process

The first step of developing this component, is writing the core
functionality of the component, in this case a bash script.

### Bash script

This is a simple script which prints a simple message, along with any
input provided to it through the `par_input` parameter. Optionally, you
can override the greeter with `par_greeter`.

``` bash
#!/usr/bin/env bash

## VIASH START

par_input="I am debug!"
par_greeter="Hello world!"

## VIASH END

echo $par_greeter $par_input
```

Anything between the `## VIASH START` and `## VIASH END` lines will
automatically be replaced at runtime with parameter values from the CLI.
Anything between these two lines can be used to test the script without
viash:

``` bash
./hello_world.sh
```

    ## Hello world! I am debug!

Next, we write a meta-file describing the functionality of this
component in YAML format.

### functionality.yaml

This file describes the general functionality of the component, its
inputs, outputs and arguments, which resources are required to run it,
as well as any test scripts to verify whether the component works
correctly. For each argument, you must specify the name and type, and
optionally provide a description, a default value, and a few more
settings.

``` yaml
name: hello_world
description: A very simple 'Hello world' component.
arguments:
- type: string
  name: input
  multiple: true
  multiple_sep: " "
- type: string
  name: --greeter
  default: "Hello world!"
resources:
- type: bash_script
  path: hello_world.sh
tests:
- type: bash_script
  path: test_hello_world.sh
```

For more information regarding the functionality YAML, see
[functionality.md](functionality.md).

### platform\_docker.yaml

If no platform yaml is provided, viash will execute the component
natively on the host environment. In order to run the component with a
Docker backend, the platform yaml needs to be provided. This file
describes the dependencies of the component, that is, the base docker
image and if-need-be extra dependencies such as R or Python packages.

This component requires very little requirements, so the file is very
simple.

``` yaml
type: docker
image: bash:4.0
```

For more information regarding the platform YAML, see
[platform.md](platform.md).

### Testing the component

Finally, writing a unit test for a viash component is relatively simple.
You just need to write a Bash script (or R, or Python) which runs the
executable multiple times, and verifies the output. Take note that the
test needs to produce an error code not equal to 0 when a mistake is
found.

``` bash
#!/usr/bin/env bash
set -ex # exit the script when one of the checks fail.

# check 1
echo ">>> Checking whether output is correct"
./hello_world I am viash! > output.txt

[[ ! -f output.txt ]] && echo "Output file could not be found!" && exit 1
grep -q 'Hello world! I am viash!' output.txt

# check 2
echo ">>> Checking whether output is correct when no parameters are given"
./hello_world > output2.txt

[[ ! -f output2.txt ]] && echo "Output file could not be found!" && exit 1
grep -q 'Hello world!' output2.txt

echo ">>> Test finished successfully!"

# check 3
echo ">>> Checking whether output is correct when more parameters are given"
./hello_world General Kenobi. --greeter="Hello there." > output3.txt

[[ ! -f output3.txt ]] && echo "Output file could not be found!" && exit 1
grep -q 'Hello there. General Kenobi.' output3.txt

echo ">>> Test finished successfully!"
```

When running the test, viash will automatically build an executable and
place it – along with other resources and test resources – in a
temporary working directory.
