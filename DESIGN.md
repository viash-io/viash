# Design Principles

## Situation

John wants to participate in the Titanic Kaggle competition and decides to explore the training set (`train.csv`). He starts developing functionality for extracting information from that file by means of simple filtering on features are reflected by the header.

John starts developing in R and calls the source file `code.R`.

Later, he would like to extend the functionality of this code and reuse it in another project. This requires him to make sure filtering conditions can be specified as an argument somehow. John is not an expert in parsing command line arguments, but he does know how to format a YAML file.

## Scenario 1

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

Please note:

1. This YAML description contains all relevant information on _what_ to run and what are the parameters to do so.
2. In the `command`, we specify a pointer to the `resources` section.

John rewrites his `code.R` file making clear what are the variables:

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

This code runs as-is, changing the parameters is a matter of updating them in the block where they are defined.

Note that we consistently use the `par$...` notation for later convenience.

## Argument Parsing Code

In order to improve the code, John uses `helpme` to convert `code.R` to `code_parsed.R` using `portash.yaml` as a config file:

```sh
helpme portash.yaml code.R > code_parsed.R
```

That results in the following file:

```r
ibrary(magrittr)
library(readr)
library(dplyr)

# PORTASH ARGPARSE
library(optparse)

arguments = commandArgs(trailingOnly=TRUE)

parser <- OptionParser(usage = "")  %>%
  add_option("--input", default = "train.csv", help = "The path to a table to be filtered.")  %>%
  add_option("--format", default = "csv", help = "The format of the input and output files.")  %>%
  add_option("--output", default = "filtered.csv", help = "The path to the output file.")  %>%
  add_option("--column_name", default = "Sex", help = "The name of the column by which to filter.")  %>%
  add_option("--value", default = "male", help = "Only rows for which the column contains this value will pass the filter.")

par <- parse_args(parser, args = arguments)
# END

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

So, this is a nice, standalone R file that takes command-line arguments but has defaults in case those are not specified.

## Scenario 2

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
```

Please note:

1. This YAML description contains all relevant information on _what_ to run and what are the parameters to do so.
2. In the `command`, we specify a pointer to the `resources` section.

John rewrites his `code.R` file making clear what are the variables:

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

This code runs as-is, changing the parameters is a matter of updating them in the block where they are defined.

Note that we consistently use the `par$...` notation for later convenience.

## Gather

In order to allow one to run this code a as a component, we convert `portash.yaml` + code to one `portash.yaml` file:

```sh
porta.sh gather portash.yaml
```

That results in the following file:

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
    body: |
      library(magrittr)
      library(readr)
      library(dplyr)

      # PORTASH ARGPARSE
      library(optparse)

      arguments = commandArgs(trailingOnly=TRUE)

      parser <- OptionParser(usage = "")  %>%
        add_option("--input", default = "train.csv", help = "The path to a table to be filtered.")  %>%
        add_option("--format", default = "csv", help = "The format of the input and output files.")  %>%
        add_option("--output", default = "filtered.csv", help = "The path to the output file.")  %>%
        add_option("--column_name", default = "Sex", help = "The name of the column by which to filter.")  %>%
        add_option("--value", default = "male", help = "Only rows for which the column contains this value will pass the filter.")

      par <- parse_args(parser, args = arguments)
      # END

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

Please note that the argparse code is automatically added.

