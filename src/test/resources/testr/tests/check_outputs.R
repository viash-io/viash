# assume tidyverse is installed
options(tidyverse.quiet = TRUE)
library(tidyverse)
library(assertthat, warn.conflicts = FALSE)

if (Sys.getenv("VIASH_PLATFORM") == "docker") {
  docker_args <- c("--data", getwd())
  data_dir <- "/data"
} else {
  docker_args <- NULL
  data_dir <- getwd()
}

cat(">>> Building component\n")
out <- processx::run("testr", "---setup")

cat(">>> Checking whether output is correct\n")
out <- processx::run("testr", c(
  "help", "--real_number", "10.5", "--whole_number=10", "-s", "you shall#not$pass",
  "--truth", "--output", paste0(data_dir, "/output.txt"), "--log", paste0(data_dir, "/log.txt"),
  "--optional", "foo", "--optional_with_default", "bar", docker_args
))

assert_that(file.exists("output.txt"))
output <- readr::read_file("output.txt")
assert_that(
  grepl('input: "help"', output),
  grepl('real_number: "10.5"', output),
  grepl('whole_number: "10"', output),
  grepl('s: "you shall#not\\$pass"', output),
  grepl('truth: "TRUE"', output),
  grepl('output: ".*/output.txt"', output),
  grepl('log: ".*/log.txt"', output),
  grepl('optional: "foo"', output),
  grepl('optional_with_default: "bar"', output)
)

assert_that(file.exists("log.txt"))
log <- readr::read_file("log.txt")
assert_that(
  grepl('Parsed input arguments.', log)
)

cat(">>> Checking whether output is correct with minimal parameters\n")
out <- processx::run("testr", c(
  "test", "--real_number", "123.456", "--whole_number=789", "-s", "my weird string", docker_args
))

output <- out$stdout
assert_that(
  grepl('input: "test"', output),
  grepl('real_number: "123.456"', output),
  grepl('whole_number: "789"', output),
  grepl('s: "my weird string"', output),
  grepl('truth: "FALSE"', output),
  grepl('optional_with_default: "The default value."', output),
  grepl('Parsed input arguments', output)
)


