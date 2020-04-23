### PORTASH START
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
### PORTASH END

log_fun <- function(..., .newline = "\n") {
  str <- paste0(..., .newline, sep = "")
  if (length(par$log) > 0) {
    write(str, file = par$log, append = TRUE)
  } else {
    cat(str)
  }
}

log_fun("Parsed input arguments.")

str <- paste0(names(par), ": ", par, "\n", collapse = "")

if (length(par$output) > 0) {
  log_fun('Writing output to file')
  write(str, par$output)
} else {
  log_fun('Printing output to console')
  cat(str)
}
