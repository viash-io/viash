# Viash

Viash is a spec and a tool for defining execution contexts and converting execution instructions to concrete instantiations.

## Toward Building Blocks of Processing

We can look at this from a myriad of angles, but let's cover a few ones:

- How many times have you downloaded a tool from the net because you wanted to try it out, only to find out that getting the dependency requirements right takes a few days?
- Have you encountered the situation where you want to combine a couple of tools in a pipeline and every tools has dedicated specs on how they should be run?
- You're developing a jupyter notebook report for a data analysis. You want your colleague to take a look but she does not have the proper tools installs. You spend 2 hours installing your Jupyter/Conda/... stack on her laptop.
- etc.

And the list indeed goes on. We thought it time to revisit the whole dependency management thing. Not just by stating that docker is the solution (it may be part of the solution) but to rethink the whole challenge from scratch.

## What can Viash do for you?

- **Pimp my script**: Given a script and some meta-information of its parameters, Viash will generate a complete CLI for you. Currently supported scripting languages are R, Python and Bash.
- **(W)rap it up**: In addition, given more meta-information on the platform on which to run it, Viash will wrap the script in an executable which uses the provided platform as backend. Currently supported platforms are Native, Docker and Nextflow.

## Examples

This README provides three examples on how to use Viash:

* Example 1: pimping a Python script
* Example 2: wrapping a Bash script
* Example 3: wrapping and running an RMarkdown script on multiple platforms

The files used in these examples can be found at [docs/examples](docs/examples).
More example files are also available at [src/test/resources](src/test/resources), which contains input files for testing Viash automatically.
 
### Example 1: pimping a Python script

Assuming a minimal Python script `code.py` which prints out any parameters it is given: 

```r
### VIASH START
par = {
  'string': 'mystring',
  'real_number': 123.987654,
  'whole_number': 17,
  'truth': True
}
### VIASH END

for key in par.keys():
	print(key + ": \"" + str(par[key]) + "\"")
```

In order for Viash to function, a minimal amount of information regarding the contents of the script needs to be provided. A minimal `functionality.yaml` could be:
```yaml
name: minimal_example
description: Prints out the parameter values.
arguments: 
- name: "--string"
  type: string
- name: "--real_number"
  type: double
- name: "--whole_number"
  type: integer
- name: "--truth"
  type: boolean_true
resources:
- type: python_script
  path: ./code.py
```

By running `viash pimp -f functionality.yaml -o wrapped.py`, Viash will inject some code into this script (in between the VIASH START and VIASH END), resulting in the following R script `wrapped.py`:

```r
### VIASH START
# The following code has been auto-generated by Viash.
import argparse

parser = argparse.ArgumentParser(
  usage = "",
  description = """Prints out the parameter values."""
)
parser.add_argument("--string", type = str, required = False)
parser.add_argument("--real_number", type = float, required = False)
parser.add_argument("--whole_number", type = int, required = False)
parser.add_argument("--truth", action='store_true', required = False)

par = vars(parser.parse_args())

### VIASH END

for key in par.keys():
	print(key + ": \"" + str(par[key]) + "\"")
```

### Example 2: wrapping a Bash script

The main strength of Viash lies in its ability to wrap an R/Python/Bash script such that it can run natively or on a Docker or Nextflow backend. 

This `code.sh` Bash script contains identical functionality in comparison to the Python example above:

```bash
#!/bin/bash

# VIASH START
par_string="myword"
par_real_number="123.987654"
par_whole_number="17"
par_truth="true"
par_output="output.txt"
# VIASH END

cat > "$par_output" << HERE
string: $par_string
real_number: $par_real_number
whole_number: $par_whole_number
truth: $par_truth
output: $par_output
HERE
```

This time, however, we will provide more extensive documentation regarding the functionality of this script.

```yaml
name: extended_example
description: | 
  Writes the given input parameters to an output file.
arguments: 
- name: "--string"
  alternatives: ["-s"]
  type: string
  description: A sentence.
  direction: input
  required: false
  default: "A default string"
- name: "--real_number"
  alternatives: ["-r"]
  type: double
  description: A real number with positional arguments.
  direction: input
  default: 123.456
  required: false
- name: "--whole_number"
  alternatives: ["-w"]
  type: integer
  description: A whole number with a standard flag.
  default: 789
  required: false
- name: "--truth"
  alternatives: ["-t"]
  type: boolean_true
  description: A switch flag.
- name: "--output"
  alternatives: ["-o"]
  type: file
  description: Write the parameters to a json file.
  direction: output
  required: true
resources:
- type: bash_script
  path: ./code.sh
```

There is much more meta-information provided, more specifically regarding each of the parameters. While it requires a bit more effort, Viash is able to create a more polished end result:

```bash
$ viash export -f functionality.yaml -o .
$ ./extended_example
--output is a required argument. Use --help to get more information on the parameters.
$ ./extended_example -h
Usage:
Writes the given input parameters to an output file.

Options:
    -s STRING, --string=STRING
        A sentence.

    -r REAL_NUMBER, --real_number=REAL_NUMBER
        A real number with positional arguments.

    -w WHOLE_NUMBER, --whole_number=WHOLE_NUMBER
        A whole number with a standard flag.

    -t, --truth
        A switch flag.

    -o OUTPUT, --output=OUTPUT
        Write the parameters to a json file.
$ ./extended_example -s foo -t -o test
$ cat test
string: foo
real_number: 123.456
whole_number: 789
truth: false
output: test
```

### Example 3: wrapping and running an RMarkdown script on multiple platforms

This example uses an R script and an RMarkdown file to generate reports. The `functionality.yaml` is defined as follows:
```yaml
name: make_vignette
description: An example for generating an RMarkdown output.
arguments: 
- name: "--title"
  alternatives: ["-t"]
  type: string
  description: A title for the plot
  direction: input
  required: false
  default: My plot
- name: "--mean"
  alternatives: ["-m"]
  type: double
  description: The mean of the distribution
  required: false
  default: 0
- name: "--sd"
  alternatives: ["-s"]
  type: double
  description: The standard deviation of the distribution
  required: false
  default: 1
- name: "--output"
  alternatives: ["-o"]
  type: file
  description: Write the parameters to a json file.
  direction: output
- name: "--format"
  alternatives: ["-f"]
  type: string
  description: The format of the output file.
  direction: input
  required: false
  default: pdf_document
  values: [pdf_document, html_document]
resources:
- type: r_script
  path: report.R
- path: report.Rmd
```

We will skip over the implementation in `report.R` and `report.Rmd`, but feel free to check them out in the example directory at [docs/examples/example 3](docs/examples/example 3).

By wrapping the script and executing it, a Rmarkdown is used to build the report:
```bash
$ viash export -f functionality.yaml -o bin
$ bin/make_vignette -t "viash is cool" -m 10 -s 2.5 -o output/report.html -f html_document
$ bin/make_vignette -t "viash is cool" -m 10 -s 2.5 -o output/report.pdf -f pdf_document
$ ls output
report.html  report.pdf
```

However, this requires that some dependencies are installed, and with the `viash export` command as-is, Viash assumes that these dependencies are installed on the system.

#### Running an executable natively
By providing a yaml detailing the requirements of your functionality, Viash can help you install requirements:
```yaml
type: native
r:
  packages: 
  - optparse
  - rmarkdown
  - tidyverse
```

```bash
$ viash export -f functionality.yaml -p platform_native.yaml -o bin
$ bin/make_vignette ---setup
Installing 1 packages: optparse
...
Adding ‘optparse_1.6.6_R_x86_64-redhat-linux-gnu.tar.gz’ to the cache
Skipping install of 'rmarkdown' from a cran remote, the SHA1 (2.1) has not changed since last install.
  Use `force = TRUE` to force installation
Skipping install of 'tidyverse' from a cran remote, the SHA1 (1.3.0) has not changed since last install.
  Use `force = TRUE` to force installation
```

#### Running an executable with a Docker container
It is likely that some systems will encounter some errors when installing packages natively. It is therefore a good practice to use Docker as a backend, to ensure better reproducability.

Here, you will have to specify a Docker image to start from (in this case `rocker/tidyverse`). If there are still additional dependencies that are not installed in the container, you can still specify them as before.
```yaml
type: docker
image: rocker/tidyverse
r:
  packages: 
  - optparse
  - rmarkdown
  - tidyverse
volumes:
- name: data
  mount: /data
workdir: /app
```

```bash
$ viash export -f functionality.yaml -p platform_docker.yaml -o bin

$ bin/make_vignette -t "viash is cool" -m 10 -s 2.5 -o /data/report.html -f html_document --data output
Unable to find image 'viash_autogen/make_vignette:latest' locally
docker: Error response from daemon: pull access denied for viash_autogen/make_vignette, repository does not exist or may require 'docker login': denied: requested access to the resource is denied.
See 'docker run --help'.

$ bin/make_vignette ---setup
Step 1/2 : FROM rocker/tidyverse
 ---> 27c035ef200a
Step 2/2 : RUN Rscript -e 'if (!requireNamespace("remotes")) install.packages("remotes")' &&   Rscript -e 'remotes::install_cran(c("optparse", "rmarkdown", "tidyverse"), repos = "https://cran.rstudio.com")'
 ---> Running in 01a387541b6a
...
Removing intermediate container 01a387541b6a
 ---> 1128ae752ae7
Successfully built 1128ae752ae7
Successfully tagged viash_autogen/make_vignette:latest

$ $ bin/make_vignette -t "viash is cool" -m 10 -s 2.5 -o /data/report.html -f html_document --data output
```

Note that in the last command, a `data` volume needs to be specified, and that the path of the output file should be using the pathname specified in the `platform_docker.yaml`.

## `NextFlowTarget` specific info

This target look at the `ftype` attribute. Let us take a look at the different possibilities with an example of each:

### `todir`

One command generates a number of files, irrespective of the fact if all these files are relevant/necessary or not.

For instance: I have a `tgz` bundle that needs to be untarred, but I only need the contents of a subdirectory. I'm aware there is an option for `tar` to extract subdirs, but let us assume for a moment this option does not exist.

We let `tar` extract the archive to a directory. The output of the module is a Channel containing and array as such:

```
[ sampleID, [ file1, file2, dir1, ...], configMap ]
```

In other words, the path element of the output triplet is an array of paths. We can flatten this nested array using something like this:

```groovy
output_ \
  | flatMap{ it ->
      it.collect{ p -> [ it[0], p, it[2] ] } }
```

### `transform`

Modify a file, the output is of the same format as the input.

For instance: add additional annotations in a `.h5ad` file.

### `convert`

Convert from one format to an other.

An example is: unzip a file.



