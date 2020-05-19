# assume tidyverse is installed
options(tidyverse.quiet = TRUE)
library(tidyverse)
library(testthat, warn.conflicts = FALSE)

if (identical(Sys.getenv("VIASH_PLATFORM"), "docker")) {
  docker_args <- c("--data", getwd())
  data_dir <- "/data"
} else {
  docker_args <- NULL
  data_dir <- getwd()
}

test_that("Building component", {
  out <- processx::run("testr", "---setup")
})


test_that("Checking whether output is correct", {
  out <- processx::run("testr", c(
    "help", "--real_number", "10.5", "--whole_number=10", "-s", "you shall#not$pass",
    "--truth", "--optional", "foo", "--optional_with_default", "bar",
    "--output", paste0(data_dir, "/output.txt"), "--log", paste0(data_dir, "/log.txt"), docker_args
  ))
  
  expect_true(file.exists("output.txt"))
  
  output <- readr::read_file("output.txt")
  expect_match(output, 'input: "help"')
  expect_match(output, 'input: "help"')
  expect_match(output, 'real_number: "10.5"')
  expect_match(output, 'whole_number: "10"')
  expect_match(output, 's: "you shall#not\\$pass"')
  expect_match(output, 'truth: "TRUE"')
  expect_match(output, 'output: ".*/output.txt"')
  expect_match(output, 'log: ".*/log.txt"')
  expect_match(output, 'optional: "foo"')
  expect_match(output, 'optional_with_default: "bar"')
  
  expect_true(file.exists("log.txt"))
  log <- readr::read_file("log.txt")
  expect_match(log, 'Parsed input arguments.')
})


test_that("Checking whether output is correct with minimal parameters", {
  out <- processx::run("testr", c(
    "test", "--real_number", "123.456", "--whole_number=789", "-s", "my weird string", docker_args
  ))
  
  output <- out$stdout
  expect_match(output, 'input: "test"')
  expect_match(output, 'real_number: "123.456"')
  expect_match(output, 'whole_number: "789"')
  expect_match(output, 's: "my weird string"')
  expect_match(output, 'truth: "FALSE"')
  expect_match(output, 'optional_with_default: "The default value."')
  expect_match(output, 'Parsed input arguments')
})


