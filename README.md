# Viash

Viash is a spec and a tool for defining execution contexts and converting execution instructions to concrete instantiations. That's the short of it. Keep reading if you want to know the long of it...

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
