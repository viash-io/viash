#' __merge__: [., ../common.yaml, ../common-runners.yaml]
#' name: test_languages_r
#' engines:
#' - type: native
#' - type: docker
#'   image: rocker/tidyverse:3.6
#'   setup:
#'     - type: r
#'       cran: optparse
#'       github: tidyverse/glue@main

## VIASH START
par <- list(
  multiple = c("one", "two"),
  multiple_pos = c("a", "b", "c", "d", "e", "f"),
  output = character(0),
  log = character(0)
)
## VIASH END

write_fun <- function(file, ...) {
  str <- paste(paste0(..., sep = ""), collapse = "")
  if (length(file) > 0) {
    write(sub("\n$", "", str), file = file, append = TRUE)
  } else {
    cat(str)
  }
}

write_fun(par$log, "INFO:Parsed input arguments.\n")

if (length(par$output) > 0) {
  write_fun(par$log, 'INFO:Writing output to file\n')
} else {
  write_fun(par$log, 'INFO:Printing output to console\n')
}

str <- sapply(names(par), function(n) {
  if(typeof(par[[n]]) == "character" && length(par[[n]]) == 0) {
    paste0(n, ": |empty array|\n")
  } else if (is.logical(par[[n]])) {
    paste0(n, ": |", tolower(paste(par[[n]], collapse = ";")), "|\n")
  } else {
    paste0(n, ": |", paste(par[[n]], collapse = ";"), "|\n")
  }
})
write_fun(par$output, str)

con = file(par[["input"]], "r")
str = paste0("head of input: |", readLines(con, n = 1), "|\n")
write_fun(par$output, str)

con = file("resource1.txt", "r")
str = paste0("head of resource1: |", readLines(con, n = 1), "|\n")
write_fun(par$output, str)

str <- sapply(names(meta), function(n) {
  paste0("meta_", n, ": |", paste(meta[[n]], collapse = ";"), "|\n")
})
write_fun(par$output, str)
