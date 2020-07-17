### VIASH START
par <- list(
  input = "input.txt",
  real_number = 123.456,
  whole_number = 789,
  s = "...",
  truth = TRUE,
  output = "output.txt",
  log = "log.txt",
  optional = "help",
  optional_with_default = "me"
)
### VIASH END
par$resources_dir <- resources_dir

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
