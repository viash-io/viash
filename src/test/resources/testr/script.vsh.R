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
#'     direction: log
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
#'   tests:
#'   - type: r_script
#'     path: tests/check_outputs.R
#' platforms:
#' - type: native
#'   r:
#'     cran: 
#'     - optparse
#' - type: docker
#'   image: rocker/tidyverse
#'   target_image: "viash_test_r"
#'   r:
#'     cran: 
#'     - optparse
#'     github:
#'     - dynverse/dynutils@devel
#'     bioc:
#'     - limma
#'   apt:
#'     packages:
#'     - libhdf5-serial-dev
#' - type: nextflow
#'   image: rocker/tidyverse

write_fun <- function(file, ...) {
  str <- paste0(..., sep = "")
  if (length(file) > 0) {
    write(sub("\n$", "", str), file = file, append = TRUE)
  } else {
    cat(str)
  }
}

write_fun(par$log, "INFO:Parsed input arguments.\n")

str <- paste0(names(par), ": |", par, "|\n", collapse = "")

if (length(par$output) > 0) {
  write_fun(par$log, 'INFO:Writing output to file\n')
} else {
  write_fun(par$log, 'INFO:Printing output to console\n')
}
write_fun(par$output, str)
