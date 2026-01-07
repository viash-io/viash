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
  current_section <- NULL
  
  while (i <= length(lines)) {
    line <- trimws(lines[i], which = "right")
    
    # Skip empty lines and comments
    if (nchar(trimws(line)) == 0 || grepl("^\\s*#", line)) {
      i <- i + 1
      next
    }
    
    # Check for top-level sections (section name followed by colon)
    if (grepl("^([a-zA-Z_][a-zA-Z0-9_]*):\\s*$", line)) {
      section_match <- regmatches(line, regexec("^([a-zA-Z_][a-zA-Z0-9_]*):\\s*$", line))[[1]]
      if (length(section_match) >= 2) {
        current_section <- section_match[2]
        result[[current_section]] <- list()
        i <- i + 1
        next
      }
    }
    
    # Check for key-value pairs using base R regex
    if (grepl("^(\\s*)([^:]+):\\s*(.*)", line)) {
      matches <- regmatches(line, regexec("^(\\s*)([^:]+):\\s*(.*)", line))[[1]]
      
      if (length(matches) >= 4) {
        indent <- matches[2]
        key <- trimws(matches[3])
        value <- trimws(matches[4])
        
        if (nchar(value) == 0) {
          # This might be the start of an array - look ahead for array items
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
            # Store the array in the current section or root
            if (!is.null(current_section)) {
              result[[current_section]][[key]] <- array_items
            } else {
              result[[key]] <- array_items
            }
            i <- j
            next
          } else {
            # Empty value
            if (!is.null(current_section)) {
              result[[current_section]][[key]] <- NULL
            } else {
              result[[key]] <- NULL
            }
            i <- i + 1
            next
          }
        } else {
          # Regular key-value pair - store in current section or root
          parsed_value <- .parse_yaml_value(value)
          if (!is.null(current_section)) {
            result[[current_section]][[key]] <- parsed_value
          } else {
            result[[key]] <- parsed_value
          }
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
