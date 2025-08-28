#!/usr/bin/env Rscript

viash_parse_yaml <- function(yaml_content = NULL) {
  #' Parse simple YAML into an R named list
  #'
  #' This function reads YAML content and converts it into an R named list.
  #' Arrays are converted to R vectors/lists.
  #'
  #' @param yaml_content Character string containing YAML content. 
  #'                     If NULL, reads from stdin.
  #' @return A named list containing the parsed YAML data
  #'
  #' The YAML format expected is simple:
  #'   key: value
  #'   array_key:
  #'     - item1
  #'     - item2
  
  if (is.null(yaml_content)) {
    # Read from stdin
    yaml_content <- paste(readLines(file("stdin")), collapse = "\n")
  }
  
  result <- list()
  lines <- strsplit(yaml_content, "\n")[[1]]
  i <- 1
  
  while (i <= length(lines)) {
    line <- trimws(lines[i], which = "right")
    
    # Skip empty lines and comments
    if (nchar(trimws(line)) == 0 || grepl("^\\s*#", line)) {
      i <- i + 1
      next
    }
    
    # Check for key-value pairs using base R regex
    if (grepl("^(\\s*)([^:]+):\\s*(.*)", line)) {
      matches <- regmatches(line, regexec("^(\\s*)([^:]+):\\s*(.*)", line))[[1]]
      
      if (length(matches) >= 4) {
        indent <- matches[2]
        key <- trimws(matches[3])
        value <- trimws(matches[4])
        
        if (nchar(value) == 0) {
          # This might be the start of an array
          # Look ahead to see if next lines are array items
          j <- i + 1
          array_items <- c()
          
          while (j <= length(lines)) {
            next_line <- trimws(lines[j], which = "right")
            
            if (nchar(trimws(next_line)) == 0) {
              j <- j + 1
              next
            }
            
            # Check if it's an array item
            if (grepl("^(\\s*)-\\s*(.*)", next_line)) {
              array_matches <- regmatches(next_line, regexec("^(\\s*)-\\s*(.*)", next_line))[[1]]
              
              if (length(array_matches) >= 3) {
                item_indent <- array_matches[2]
                item_value <- trimws(array_matches[3])
                
                # Make sure it's indented more than the key
                if (nchar(item_indent) > nchar(indent)) {
                  array_items <- c(array_items, .parse_yaml_value(item_value))
                  j <- j + 1
                  next
                }
              }
            }
            break
          }
          
          if (length(array_items) > 0) {
            result[[key]] <- array_items
            i <- j
            next
          } else {
            # Empty value
            result[[key]] <- NULL
            i <- i + 1
            next
          }
        } else {
          result[[key]] <- .parse_yaml_value(value)
          i <- i + 1
          next
        }
      }
    }
    
    i <- i + 1
  }
  
  return(result)
}

.parse_yaml_value <- function(value) {
  #' Parse a YAML value into appropriate R type
  #' @param value Character string to parse
  #' @return Parsed value with appropriate R type
  
  if (value == "null") {
    return(NULL)
  } else if (value == "true") {
    return(TRUE)
  } else if (value == "false") {
    return(FALSE)
  } else if (grepl('^"(.*)"$', value)) {
    # Quoted string - unescape
    unquoted <- substr(value, 2, nchar(value) - 1)
    unquoted <- gsub('\\\\"', '"', unquoted)
    unquoted <- gsub('\\\\n', '\n', unquoted)
    unquoted <- gsub('\\\\\\\\', '\\\\', unquoted)
    return(unquoted)
  } else if (grepl("^-?\\d+$", value)) {
    # Integer
    return(as.integer(value))
  } else if (grepl("^-?\\d*\\.\\d+$", value)) {
    # Numeric (double)
    return(as.numeric(value))
  } else {
    # Unquoted string
    return(value)
  }
}

# If run as script, parse YAML from stdin and print result
if (!interactive() && identical(environment(), globalenv())) {
  result <- viash_parse_yaml()
  
  # Print in a format that can be sourced in R
  cat("# Parsed YAML parameters:\n")
  for (name in names(result)) {
    value <- result[[name]]
    if (is.null(value)) {
      cat(sprintf("%s <- NULL\n", name))
    } else if (is.logical(value) && length(value) == 1) {
      cat(sprintf("%s <- %s\n", name, ifelse(value, "TRUE", "FALSE")))
    } else if (is.numeric(value) && length(value) == 1) {
      cat(sprintf("%s <- %s\n", name, value))
    } else if (is.character(value) && length(value) == 1) {
      cat(sprintf("%s <- %s\n", name, deparse(value)))
    } else {
      # Vector or list
      cat(sprintf("%s <- %s\n", name, deparse(value)))
    }
  }
}
