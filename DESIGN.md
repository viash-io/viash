# Design Principles

## Situation

John wants to participate in the Titanic Kaggle competition and decides to explore the training set (`train.csv`). He starts developing functionality for extracting information from that file by means of simple filtering on features are reflected by the header.

John starts developing in R and calls the source file `code.R`.

Later, he would like to extend the functionality of this code and reuse it in another project. This requires him to make sure filtering conditions can be specified as an argument somehow. John is not an expert in parsing command line arguments, but he does know how to format a YAML file.

## Prerequisites

`YAML` file called `portash.yaml`:

```yaml
function:
  name: generate_report
  description: |
    Generate a report from the input data.
  command: Rscript {{resources.code}}
  parameters:
    - name: input
      type: file
      description: The path to a table to be filtered.
      must_exist: true
      default: train.csv
    - name: format
      type: string
      description: The format of the input files.
      default: csv
      values: [csv, tsv, rds, h5]
    - name: output
      type: file
      description: The path to the output file.
      default: report.html
    - name: output_format
      type: string
      description: The format of the report.
      default: html
      values: [pdf, html]
resources:
  - name: code
    path: ./code.R
  - name: report
    path: ./report.Rmd
```

He rewrites his `code.R` file making clear what are the variables:

```r
library(magrittr)
library(readr)
library(dplyr)

par <- list(
  format = "csv",
  input = "train.csv",
  output = "filtered.csv"
  column_name = "Sex",
  value = "male"
)

# Import data
read_fun <- switch(
  par$format,
  "csv" = readr::read_csv,
  "tsv" = readr::read_tsv,
  "rds" = readr::read_rds,
  "h5" = stop("h5 files are currently not supported.")
)
table <- read_fun(par$input)

# Filter data
filtered_table <-
  table %>%
  filter(.data[[par$column_name]] == par$value)

# Write data to file
write_fun <- switch(
  par$format,
  "csv" = readr::write_csv,
  "tsv" = readr::write_tsv,
  "rds" = readr::write_rds,
  "h5" = stop("h5 files are currently not supported.")
)
write_fun(filtered_table, par$output)
```


