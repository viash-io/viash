library(testthat)

# create dummy input file
old_lines <- c("one", "two", "three")
writeLines(old_lines, "input.txt")

# run executable
system("./EXECUTABLE --input input.txt --output output.txt --option FOO")

# check whether output file exists
expect_true(file.exists("output.txt"))

# check whether content matches expected content
expected_lines <- c("FOO-one", "FOO-two", "FOO-three")
new_lines <- readLines("output.txt")
expect_equal(new_lines, expected_lines)

cat(">>> Test finished successfully!")
