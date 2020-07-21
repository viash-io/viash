### VIASH START
par <- list(
  title = "a title for my plot",
  mean = 0,
  sd = 1,
  output = "output.pdf",
  format = "pdf_document"
)
resources_dir <- "."
### VIASH END

# get absolute path to file
path <- normalizePath(par$output, mustWork = FALSE)

# render markdown
rmarkdown::render(
  input = file.path(resources_dir, "report.Rmd"), 
  output_file = basename(path),
  output_dir = dirname(path),
  output_format = par$format,
  params = par[c("title", "mean", "sd")]
)
