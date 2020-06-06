# Viash - NextFlow Example

## Introduction

This example deals with creating a _module_ that can be incorporated in a larger NextFlow pipeline.

We reuse a simple filter script in R.

## Viash

From the example directory, run the following:

```sh
viash export -f functionality.yaml -p platform_docker.yaml -o export/setup/
viash export -f functionality.yaml -p platform_nextflow.yaml -o export/nextflow
export/setup/filter_table ---setup
```

## Running the Module

We are now ready to run the module and we use the example `train.csv` data:

```sh
cd export/nextflow
NXF_VER=20.04.1-edge nextflow run . \
   --input $VIASH/data/train.csv \
   --filter_table__column_name "Sex" \
   --filter_table__value "female"
```

We assume `$VIASH` points to your local `viash` repository.

The resulting output can be found under `out/`.
