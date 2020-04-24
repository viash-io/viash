# Viash

Viash is a spec and a tool for defining execution contexts and converting execution instructions to concrete instantiations. That's the short of it. Keep reading if you want to know the long of it...

## Toward Building Blocks of Processing

We can look at this from a myriad of angles, but let's cover a few ones:

- How many times have you downloaded a tool from the net because you wanted to try it out, only to find out that getting the dependency requirements right takes a few days?
- Have you encountered the situation where you want to combine a couple of tools in a pipeline and every tools has dedicated specs on how they should be run?
- You're developing a jupyter notebook report for a data analysis. You want your colleague to take a look but she does not have the proper tools installs. You spend 2 hours installing your Jupyter/Conda/... stack on her laptop.
- etc.

And the list indeed goes on. We thought it time to revisit the whole dependency management thing. Not just by stating that docker is the solution (it may be part of the solution) but to rethink the whole challenge from scratch.

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

## The _what_ and _how_?

... tbc ...

## Example

Say, this is the Viash spec:

```yaml
contexts:
  docker:
    image: r-container
    volumes:
      $PWD: $PWD
    workdir:
      $PWD
```

Together with the following Portash spec:

```yaml
function:
  name: filter_gender
  command: Rscript code.R
  parameters:
    input: train.csv
    output: filtered.csv
    gender: male
```

This can be converted as follows:

```sh
via.sh --config viash.yaml --in portash.yaml --out run.sh
```

in order to create a `run.sh` script as follows:

```sh
#!/bin/sh

docker run -it -v $PWD:$PWD -w $PWD r-container \
  Rscript code.R --input train.csv --output filtered.csv --gender male
```

## Implementation Ideas

### Templating

Much of what needs to be done, especially for the more complicated execution environments, is basically templating. Let's consider [mustache](http://mustache.github.io/) for a second. There are command-line implementations of it. That means, a template like the following can already convert a Portash spec 

```yaml
command: Rscript script.R
parameters:
  - name: input
    value: inputfile
  - name: output
    value: outputfile
  - name: gender
    value: male
```

and a simple template:

```
#!/bin/bash

{{function.command}} {{#function.parameters}}--{{name}} {{value}} {{/function.parameters}}
```

into a runnable script:

```sh
#!/bin/bash

Rscript script.R --input inputfile --output outputfile --gender male
```

This is a fairly easy example, but it does get the idea across. The template can be found under `templates/portash_to_script.template`.

__Please note__: We format the parameters a bit differently in order to make the Mustache processing a bit more standard.

Now, part of what Portash (the script) does is exactly this: running a Portash declaration. But it does not have to be Portash and this shows we can generalize it.

Please note that for this kind of transformation, no additional runtime information is required. Let us now handle a case where Docker is involved using the same ideas, but extending them a bit.

A typical Mustache workflow takes one YAML for the variables and one template. In order for the Portash and Viash declarations to both be read in one go, we merge them. Let's see...

Take the following combined spec:

```yaml
function:
  command: Rscript code.R
  parameters:
    - name: input
      value: train.csv
    - name: output
      value: filtered.csv
    - name: gender
      value: male
env:
  contexts:
    docker:
      image: r-container
      volumes:
        - from: $PWD
          to: $PWD
      workdir: $PWD

```

Running this with the following template:

```
#!/bin/bash

docker run -i \
  {{#env.contexts.docker.volumes}}-v "{{from}}:{{to}}" {{/env.contexts.docker.volumes}} \
  -w "{{env.contexts.docker.workdir}}" \
  {{env.contexts.docker.image}} \
  {{function.command}} {{#function.parameters}}--{{name}} {{value}} {{/function.parameters}}

```

yields this script:

```sh
#!/bin/bash

docker run -i \
  -v "$PWD:$PWD"  \
  -w "$PWD" \
  r-container \
  Rscript script.R --input inputfile --output outputfile --gender male
```

Running this script is easy:

```sh
~/go/bin/mustache-cli test.yaml portash_docker_script.template
```

`mustache-cli` is the CLI tool we use for templating, `test.yaml` is the YAML file presented above.



### Ideas for a Scala implementation

- Argument Parsing: <https://github.com/scallop/scallop>
- Templating: <https://github.com/eikek/yamusca>
- YAML Parsing: <https://github.com/circe/circe-yaml>

