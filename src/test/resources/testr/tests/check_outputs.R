library(processx, warn.conflicts = FALSE)
library(testthat, warn.conflicts = FALSE)

read_file <- function(file) {
  paste(paste0(readLines(file), "\n"), collapse = "")
}

test_that("Checking whether output is correct", {
  out <- processx::run("./testr", c(
    "help", "--real_number", "10.5", "--whole_number=10", "-s", "a string with spaces",
    "--truth", "--optional", "foo", "--optional_with_default", "bar",
    "a", "b", "c", "d",
    "--output", "./output.txt", "--log", "./log.txt",
    "--multiple", "one", "--multiple=two", 
    "e", "f"
  ))
  expect_true(file.exists("output.txt"))
  
  output <- read_file("output.txt")
  expect_match(output, 'input: \\|help\\|')
  expect_match(output, 'real_number: \\|10.5\\|')
  expect_match(output, 'whole_number: \\|10\\|')
  expect_match(output, 's: \\|a string with spaces\\|')
  expect_match(output, 'truth: \\|TRUE\\|')
  expect_match(output, 'output: \\|.*/output.txt\\|')
  expect_match(output, 'log: \\|.*/log.txt\\|')
  expect_match(output, 'optional: \\|foo\\|')
  expect_match(output, 'optional_with_default: \\|bar\\|')
  expect_match(output, 'multiple: \\|c\\("one", "two"\\)\\|')
  expect_match(output, 'multiple_pos: \\|c\\("a", "b", "c", "d", "e", "f"\\)\\|')
  expect_match(output, 'meta_resources_dir: \\|..*\\|')
  expect_match(output, 'meta_functionality_name: \\|testr\\|')
  
  expect_true(file.exists("log.txt"))
  log <- read_file("log.txt")
  expect_match(log, 'Parsed input arguments.')
})


test_that("Checking whether output is correct with minimal parameters", {
  out <- processx::run("./testr", c(
    "test", "--real_number", "123.456", "--whole_number=789", "-s", 'my$weird#string"""\'\'\'`'
  ))
  
  output <- out$stdout
  expect_match(output, 'input: \\|test\\|')
  expect_match(output, 'real_number: \\|123.456\\|')
  expect_match(output, 'whole_number: \\|789\\|')
  expect_match(output, 's: \\|my\\$weird#string"""\'\'\'`\\|')
  expect_match(output, 'truth: \\|FALSE\\|')
  expect_match(output, 'optional_with_default: \\|The default value.\\|')
  expect_match(output, 'Parsed input arguments')
  expect_match(output, 'multiple: \\|NULL\\|')
  expect_match(output, 'multiple_pos: \\|NULL\\|')
  expect_match(output, 'meta_resources_dir: \\|..*\\|')
  expect_match(output, 'meta_functionality_name: \\|testr\\|')
})

test_that("Checking whether executable fails when wrong parameters are given", {
  out <- processx::run("./testr", error_on_status = FALSE, c(
    "test", "--real_number", "abc", "--whole_number=abc", "-s", "my weird string", "--derp"
  ))
  print(out)
  
  expect_gt(out$status, 0)
})


