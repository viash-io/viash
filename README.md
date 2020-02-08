# Viash

Viash is a spec and a tool for defining execution contexts and converting execution instructions to concrete instantiations. That's the short of it. Keep reading if you want to know the long of it...

## Toward Building Blocks of Processing

We can look at this from a myriad of angles, but let's cover a few ones:

- How many times have you downloaded a tool from the net because you wanted to try it out, only to find out that getting the dependency requirements right takes a few days?
- Have you encountered the situation where you want to combine a couple of tools in a pipeline and every tools has dedicated specs on how they should be run?
- You're developing a jupyter notebook report for a data analysis. You want your colleague to take a look but she does not have the proper tools installs. You spend 2 hours installing your Jupyter/Conda/... stack on her laptop.
- etc.

And the list indeed goes on. We thought it time to revisit the whole dependency management thing. Not just by stating that docker is the solution (it may be part of the solution) but to rethink the whole challenge from scratch.

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
