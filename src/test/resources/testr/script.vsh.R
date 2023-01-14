#' functionality:
#'   name: testr
#'   description: |
#'     Prints out the parameter values.
#'     Checking what happens with multiline descriptions.
#'   arguments:
#'   - name: "input"
#'     type: file
#'     description: An input file with positional arguments.
#'     direction: input
#'     required: true
#'   - name: "--real_number"
#'     type: double
#'     description: A real number with positional arguments.
#'     required: true
#'   - name: "--whole_number"
#'     type: integer
#'     description: A whole number with a standard flag.
#'     required: true
#'   - name: "--long_number"
#'     type: long
#'     description: A long
#'   - name: "-s"
#'     type: string
#'     description: A sentence or word with a short flag.
#'     required: true
#'   - name: "--truth"
#'     type: boolean_true
#'     description: A switch flag.
#'   - name: "--output"
#'     alternatives: ["-o"]
#'     type: file
#'     description: Write the parameters to a json file.
#'     direction: output
#'   - name: "--log"
#'     type: file
#'     description: An optional log file.
#'     direction: output
#'   - name: "--optional"
#'     type: string
#'     description: An optional string.
#'   - name: "--optional_with_default"
#'     type: string
#'     default: "The default value."
#'   - name: "--multiple"
#'     type: string
#'     multiple: true
#'   - name: "multiple_pos"
#'     type: string
#'     multiple: true
#'   test_resources:
#'   - type: bash_script
#'     path: test.sh
#' platforms:
#' - type: native
#' - type: docker
#'   image: rocker/tidyverse:3.6
#'   setup:
#'     - type: r
#'       cran: optparse
#'       github: tidyverse/glue@main
#' - type: nextflow

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
  paste0(n, ": |", paste(par[[n]], collapse = ","), "|\n")
})
write_fun(par$output, str)

str <- sapply(names(meta), function(n) {
  paste0("meta_", n, ": |", paste(meta[[n]], collapse = ","), "|\n")
})
write_fun(par$output, str)
