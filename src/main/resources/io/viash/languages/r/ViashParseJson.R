#' Parse JSON parameters file into an R list
#'
#' @param json_path Path to the JSON file. If NULL, reads from $VIASH_WORK_PARAMS environment variable.
#' @return Named list containing the parsed JSON data.
viash_parse_json <- function(json_path = NULL) {
  if (is.null(json_path)) {
    json_path <- Sys.getenv("VIASH_WORK_PARAMS")
    if (json_path == "") {
      stop("VIASH_WORK_PARAMS environment variable not set")
    }
  }
  
  if (!file.exists(json_path)) {
    stop(paste0("Parameters file not found: ", json_path))
  }
  
  tryCatch({
    json_text <- readLines(json_path, warn = FALSE)
    json_text <- paste(json_text, collapse = "\n")
    # Use bigint_as_char = TRUE to preserve large integers as strings
    # This prevents precision loss for values > 2^53
    jsonlite::parse_json(json_text, simplifyVector = TRUE, bigint_as_char = TRUE)
  }, error = function(e) {
    stop(paste0("Error parsing JSON file: ", e$message))
  })
}
