#' Parse JSON parameters file into an R list
#'
#' Recursive descent JSON parser with no external dependencies.
#' Matches the structure of the Scala ViashJsonParser.
#'
#' @param json_path Path to the JSON file. If NULL, reads from
#'   $VIASH_WORK_PARAMS environment variable.
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

  json_text <- paste(readLines(json_path, warn = FALSE), collapse = "\n")
  .viash_json_parse(json_text)
}

# Recursive descent JSON parser.
# Pre-splits input into a character vector for O(1) indexing.
# Returns nested R lists with vector simplification for homogeneous arrays.
.viash_json_parse <- function(json) {
  chars <- strsplit(json, "")[[1]]
  len <- length(chars)
  pos <- 1L

  # Lookup table for single-character JSON escape sequences
  escape_map <- list(
    "n" = "\n", "t" = "\t", "r" = "\r",
    "b" = "\b", "f" = "\f",
    "\\" = "\\", "\"" = "\"", "/" = "/"
  )

  skip_ws <- function() {
    while (pos <= len) {
      ch <- chars[pos]
      if (ch == " " || ch == "\t" || ch == "\n" || ch == "\r") {
        pos <<- pos + 1L
      } else {
        return(invisible(NULL))
      }
    }
  }

  peek <- function() {
    skip_ws()
    if (pos > len) stop("viash_parse_json: Unexpected end of JSON")
    chars[pos]
  }

  consume <- function(expected) {
    skip_ws()
    if (pos > len) {
      stop(paste0("viash_parse_json: Expected '", expected,
                   "' but reached end of JSON"))
    }
    if (chars[pos] != expected) {
      stop(paste0("viash_parse_json: Expected '", expected,
                   "' at position ", pos, ", got '", chars[pos], "'"))
    }
    pos <<- pos + 1L
  }

  parse_value <- function() {
    ch <- peek()
    if (ch == "\"") return(parse_string())
    if (ch == "{")  return(parse_object())
    if (ch == "[")  return(parse_array())
    if (ch == "t" || ch == "f") return(parse_boolean())
    if (ch == "n")  return(parse_null())
    if (ch == "-" || (ch >= "0" && ch <= "9")) return(parse_number())
    stop(paste0("viash_parse_json: Unexpected character '", ch,
                "' at position ", pos))
  }

  parse_string <- function() {
    consume("\"")
    buf <- character(min(len - pos + 1L, 1024L))
    buf_len <- 0L

    while (pos <= len) {
      ch <- chars[pos]
      if (ch == "\"") {
        pos <<- pos + 1L
        if (buf_len == 0L) return("")
        return(paste0(buf[seq_len(buf_len)], collapse = ""))
      }
      buf_len <- buf_len + 1L
      # Grow buffer if needed
      if (buf_len > length(buf)) {
        buf <- c(buf, character(length(buf)))
      }
      if (ch == "\\") {
        pos <<- pos + 1L
        if (pos > len) stop("viash_parse_json: Unterminated string escape")
        esc <- chars[pos]
        mapped <- escape_map[[esc]]
        if (!is.null(mapped)) {
          buf[buf_len] <- mapped
        } else if (esc == "u") {
          # Unicode escape \uXXXX
          if (pos + 4L > len) {
            stop("viash_parse_json: Invalid unicode escape")
          }
          hex <- paste0(chars[(pos + 1L):(pos + 4L)], collapse = "")
          buf[buf_len] <- intToUtf8(strtoi(hex, 16L))
          pos <<- pos + 4L
        } else {
          # Unknown escape: keep escaped character as-is
          buf[buf_len] <- esc
        }
      } else {
        buf[buf_len] <- ch
      }
      pos <<- pos + 1L
    }
    stop("viash_parse_json: Unterminated string")
  }

  parse_number <- function() {
    start <- pos
    # Optional minus
    if (chars[pos] == "-") pos <<- pos + 1L
    # Integer digits
    while (pos <= len && chars[pos] >= "0" && chars[pos] <= "9") {
      pos <<- pos + 1L
    }
    # Fractional part
    has_decimal <- FALSE
    if (pos <= len && chars[pos] == ".") {
      has_decimal <- TRUE
      pos <<- pos + 1L
      while (pos <= len && chars[pos] >= "0" && chars[pos] <= "9") {
        pos <<- pos + 1L
      }
    }
    # Exponent part
    has_exp <- FALSE
    if (pos <= len && (chars[pos] == "e" || chars[pos] == "E")) {
      has_exp <- TRUE
      pos <<- pos + 1L
      if (pos <= len && (chars[pos] == "+" || chars[pos] == "-")) {
        pos <<- pos + 1L
      }
      while (pos <= len && chars[pos] >= "0" && chars[pos] <= "9") {
        pos <<- pos + 1L
      }
    }

    num_str <- paste0(chars[start:(pos - 1L)], collapse = "")
    if (has_decimal || has_exp) {
      return(as.double(num_str))
    }
    # Integer: try R's 32-bit integer first
    n <- suppressWarnings(as.integer(num_str))
    if (!is.na(n)) return(n)
    # Larger integer: use double if within exact precision range
    digits <- nchar(gsub("-", "", num_str))
    if (digits <= 15L) return(as.double(num_str))
    # Very large integer: keep as character for bit64 handling
    num_str
  }

  parse_boolean <- function() {
    remaining <- len - pos + 1L
    if (remaining >= 4L &&
        paste0(chars[pos:(pos + 3L)], collapse = "") == "true") {
      pos <<- pos + 4L
      return(TRUE)
    }
    if (remaining >= 5L &&
        paste0(chars[pos:(pos + 4L)], collapse = "") == "false") {
      pos <<- pos + 5L
      return(FALSE)
    }
    stop(paste0("viash_parse_json: Invalid boolean at position ", pos))
  }

  parse_null <- function() {
    if (len - pos + 1L >= 4L &&
        paste0(chars[pos:(pos + 3L)], collapse = "") == "null") {
      pos <<- pos + 4L
      return(NULL)
    }
    stop(paste0("viash_parse_json: Invalid null at position ", pos))
  }

  parse_array <- function() {
    consume("[")
    if (peek() == "]") {
      consume("]")
      return(list())
    }

    items <- list()
    items[1L] <- list(parse_value())
    while (peek() == ",") {
      consume(",")
      items[length(items) + 1L] <- list(parse_value())
    }
    consume("]")

    # Simplify homogeneous scalar arrays to vectors (like jsonlite simplifyVector)
    .viash_simplify_array(items)
  }

  parse_object <- function() {
    consume("{")
    if (peek() == "}") {
      consume("}")
      return(structure(list(), names = character(0)))
    }

    keys <- character(0)
    vals <- list()
    key <- parse_string()
    consume(":")
    keys <- c(keys, key)
    vals[length(vals) + 1L] <- list(parse_value())
    while (peek() == ",") {
      consume(",")
      key <- parse_string()
      consume(":")
      keys <- c(keys, key)
      vals[length(vals) + 1L] <- list(parse_value())
    }
    consume("}")

    names(vals) <- keys
    vals
  }

  parse_value()
}

# Simplify a list to a vector if all elements are the same scalar type.
.viash_simplify_array <- function(items) {
  n <- length(items)
  if (n == 0L) return(list())

  # NULL elements prevent simplification
  has_null <- vapply(items, is.null, logical(1))
  if (any(has_null)) return(items)

  # Only simplify scalar (length-1, non-list) elements
  is_scalar <- vapply(items, function(x) {
    length(x) == 1L && !is.list(x)
  }, logical(1))
  if (!all(is_scalar)) return(items)

  types <- vapply(items, function(x) class(x)[[1L]], character(1))
  unique_types <- unique(types)

  if (length(unique_types) == 1L) {
    # All same type: collapse to vector
    return(unlist(items))
  }
  if (all(unique_types %in% c("integer", "numeric"))) {
    # Mix of integer and numeric: promote to numeric
    return(as.numeric(unlist(items)))
  }

  # Mixed types: keep as list
  items
}
