library(magrittr, warn.conflicts = FALSE)
library(readr, warn.conflicts = FALSE)
library(dplyr, warn.conflicts = FALSE)

### VIASH START
par <- list(
  input = "input.csv",
  format = "csv",
  output = "output.csv",
  column_name = "Sex",
  value = "male"
)
### VIASH END

# Import data
read_fun <- switch(
  par$format,
  "csv" = readr::read_csv,
  "tsv" = readr::read_tsv,
  "rds" = readr::read_rds,
  "h5" = stop("h5 files are currently not supported.")
)
table <- read_fun(par$input)

# Filter data.
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

