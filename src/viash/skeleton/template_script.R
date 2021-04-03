cat("This is a skeleton component\n")
cat("The arguments are:\n")
cat(" - input: ", par$input, "\n", sep = "")
cat(" - output: ", par$output, "\n", sep = "")
cat(" - option: ", par$option, "\n", sep = "")
cat("\n")

cat("Reading input file\n")
lines <- readLines(par$input)

cat("Running output algorithm\n")
new_lines <- paste0(par$option, "-", lines)

cat("Writing output file\n")
writeLines(new_lines, con = par$output)
