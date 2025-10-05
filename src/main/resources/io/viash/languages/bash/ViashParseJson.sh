#!/usr/bin/env bash

# ViashParseJsonBash: Parse JSON parameters into Bash variables
#
# This function reads JSON content from stdin and exports variables for each key-value pair.
# Nested objects (level 2+) are flattened with underscore separators (e.g., par_input, meta_name).
# Arrays are exported as Bash arrays.
# Deep nesting (level 4+) is stored as JSON strings.
#
# Usage:
#   ViashParseJsonBash < json_file
#   ViashParseJsonBash <<< "$json_content"
#
# Spec: See docs/json_parser_spec.md

function ViashParseJsonBash {
  local depth=0
  local current_section=""
  local in_array=false
  local array_name=""
  local array_items=()
  local in_nested=false
  local nested_name=""
  local nested_json=""
  local nested_depth=0
  
  while IFS= read -r line; do
    # Trim whitespace
    line="${line#"${line%%[![:space:]]*}"}"
    line="${line%"${line##*[![:space:]]}"}"
    
    # Skip empty lines
    [ -z "$line" ] && continue
    
    # Handle nested object collection (depth 3+)
    if $in_nested; then
      nested_json+="$line"
      
      # Count braces to track when nested object ends
      if [[ "$line" =~ \{ ]]; then
        ((nested_depth++))
      fi
      if [[ "$line" =~ ^\}[[:space:]]*,?[[:space:]]*$ ]]; then
        ((nested_depth--))
        if [ $nested_depth -eq 0 ]; then
          # Nested object complete - store as JSON string
          local escaped="${nested_json//\'/\'\\\'\'}"
          eval "declare -g ${nested_name}='${escaped}'"
          in_nested=false
          nested_name=""
          nested_json=""
        fi
      fi
      continue
    fi
    
    # Handle array collection
    if $in_array; then
      if [[ "$line" =~ ^\][[:space:]]*,?[[:space:]]*$ ]]; then
        # End of array
        eval "declare -g -a ${array_name}=($(printf '%q ' "${array_items[@]}"))"
        in_array=false
        array_name=""
        array_items=()
      else
        # Collect array items from current line
        local items_line="$line"
        # Remove trailing comma if present
        items_line="${items_line%,}"
        
        # Split by comma (simple approach - works for most cases)
        local item
        local in_quotes=false
        local current_item=""
        
        for ((i=0; i<${#items_line}; i++)); do
          local char="${items_line:$i:1}"
          
          if [ "$char" = '"' ] && [ "${items_line:$((i-1)):1}" != '\' ]; then
            in_quotes=$( [ "$in_quotes" = "true" ] && echo "false" || echo "true" )
            current_item+="$char"
          elif [ "$char" = ',' ] && [ "$in_quotes" = "false" ]; then
            # End of item
            current_item="${current_item#"${current_item%%[![:space:]]*}"}"
            current_item="${current_item%"${current_item##*[![:space:]]}"}"
            if [ -n "$current_item" ]; then
              array_items+=("$(_viash_unescape_json_value "$current_item")")
            fi
            current_item=""
          else
            current_item+="$char"
          fi
        done
        
        # Add last item
        current_item="${current_item#"${current_item%%[![:space:]]*}"}"
        current_item="${current_item%"${current_item##*[![:space:]]}"}"
        if [ -n "$current_item" ]; then
          array_items+=("$(_viash_unescape_json_value "$current_item")")
        fi
      fi
      continue
    fi
    
    # Root level opening/closing braces
    if [[ "$line" =~ ^\{[[:space:]]*$ ]]; then
      ((depth++))
      continue
    fi
    
    if [[ "$line" =~ ^\}[[:space:]]*,?[[:space:]]*$ ]]; then
      ((depth--))
      if [ $depth -eq 1 ]; then
        current_section=""
      fi
      continue
    fi
    
    # Section header: "section": {
    if [[ "$line" =~ ^\"([^\"]+)\":[[:space:]]*\{[[:space:]]*,?[[:space:]]*$ ]]; then
      local key="${BASH_REMATCH[1]}"
      
      if [ $depth -eq 1 ]; then
        # Top-level section (par, meta, dep)
        current_section="$key"
        ((depth++))
      elif [ $depth -eq 2 ] && [ -n "$current_section" ]; then
        # Nested object - start collection
        in_nested=true
        nested_name="${current_section}_${key}"
        nested_json="$line"
        nested_depth=1
      fi
      continue
    fi
    
    # Array: "key": [
    if [[ "$line" =~ ^\"([^\"]+)\":[[:space:]]*\[(.*)$ ]]; then
      local key="${BASH_REMATCH[1]}"
      local rest="${BASH_REMATCH[2]}"
      local var_name="${current_section:+${current_section}_}${key}"
      
      # Check if array ends on same line
      if [[ "$rest" =~ ^\][[:space:]]*,?[[:space:]]*$ ]]; then
        # Empty array
        eval "declare -g -a ${var_name}=()"
      elif [[ "$rest" =~ ^(.*)\][[:space:]]*,?[[:space:]]*$ ]]; then
        # Single-line array
        local content="${BASH_REMATCH[1]}"
        content="${content%,}"
        
        # Parse array items
        local items=()
        _viash_parse_array_content "$content" items
        eval "declare -g -a ${var_name}=($(printf '%q ' "${items[@]}"))"
      else
        # Multi-line array
        in_array=true
        array_name="$var_name"
        array_items=()
        
        # Process any items on this line
        rest="${rest%,}"
        if [ -n "$rest" ]; then
          local item
          local in_quotes=false
          local current_item=""
          
          for ((i=0; i<${#rest}; i++)); do
            local char="${rest:$i:1}"
            
            if [ "$char" = '"' ] && [ "${rest:$((i-1)):1}" != '\' ]; then
              in_quotes=$( [ "$in_quotes" = "true" ] && echo "false" || echo "true" )
              current_item+="$char"
            elif [ "$char" = ',' ] && [ "$in_quotes" = "false" ]; then
              current_item="${current_item#"${current_item%%[![:space:]]*}"}"
              current_item="${current_item%"${current_item##*[![:space:]]}"}"
              if [ -n "$current_item" ]; then
                array_items+=("$(_viash_unescape_json_value "$current_item")")
              fi
              current_item=""
            else
              current_item+="$char"
            fi
          done
          
          current_item="${current_item#"${current_item%%[![:space:]]*}"}"
          current_item="${current_item%"${current_item##*[![:space:]]}"}"
          if [ -n "$current_item" ]; then
            array_items+=("$(_viash_unescape_json_value "$current_item")")
          fi
        fi
      fi
      continue
    fi
    
    # Key-value pair: "key": value
    if [[ "$line" =~ ^\"([^\"]+)\":[[:space:]]*(.+[^[:space:]])[[:space:]]*,?[[:space:]]*$ ]]; then
      local key="${BASH_REMATCH[1]}"
      local value="${BASH_REMATCH[2]}"
      local var_name="${current_section:+${current_section}_}${key}"
      
      # Remove trailing comma
      value="${value%,}"
      
      # Parse and assign value
      local unescaped="$(_viash_unescape_json_value "$value")"
      eval "declare -g ${var_name}='${unescaped//\'/\'\\\'\'}'"
      continue
    fi
  done
}

# Helper: Unescape JSON value
function _viash_unescape_json_value {
  local value="$1"
  
  # Handle different value types
  if [ "$value" = "null" ]; then
    echo "null"
  elif [ "$value" = "true" ] || [ "$value" = "false" ]; then
    echo "$value"
  elif [[ "$value" =~ ^-?[0-9]+(\.[0-9]+)?([eE][+-]?[0-9]+)?$ ]]; then
    # Number
    echo "$value"
  elif [[ "$value" =~ ^\"(.*)\"$ ]]; then
    # String - remove quotes and unescape
    local str="${BASH_REMATCH[1]}"
    
    # Unescape JSON escape sequences
    # Note: We keep \n and \t as literal strings (not actual newline/tab characters)
    # to match YAML parser behavior and test expectations
    str="${str//\\\\/\\}"      # \\ -> \
    str="${str//\\\"/\"}"      # \" -> "
    # str="${str//\\n/$'\n'}"    # \n -> newline (disabled - keep literal)
    # str="${str//\\t/$'\t'}"    # \t -> tab (disabled - keep literal)
    str="${str//\\r/$'\r'}"    # \r -> carriage return
    str="${str//\\b/$'\b'}"    # \b -> backspace
    str="${str//\\f/$'\f'}"    # \f -> form feed
    
    echo "$str"
  else
    # Fallback - return as-is
    echo "$value"
  fi
}

# Helper: Parse array content (for single-line arrays)
function _viash_parse_array_content {
  local content="$1"
  local -n result_array="$2"
  
  local in_quotes=false
  local current_item=""
  
  for ((i=0; i<${#content}; i++)); do
    local char="${content:$i:1}"
    
    if [ "$char" = '"' ] && [ "${content:$((i-1)):1}" != '\' ]; then
      in_quotes=$( [ "$in_quotes" = "true" ] && echo "false" || echo "true" )
      current_item+="$char"
    elif [ "$char" = ',' ] && [ "$in_quotes" = "false" ]; then
      # End of item
      current_item="${current_item#"${current_item%%[![:space:]]*}"}"
      current_item="${current_item%"${current_item##*[![:space:]]}"}"
      if [ -n "$current_item" ]; then
        result_array+=("$(_viash_unescape_json_value "$current_item")")
      fi
      current_item=""
    else
      current_item+="$char"
    fi
  done
  
  # Add last item
  current_item="${current_item#"${current_item%%[![:space:]]*}"}"
  current_item="${current_item%"${current_item##*[![:space:]]}"}"
  if [ -n "$current_item" ]; then
    result_array+=("$(_viash_unescape_json_value "$current_item")")
  fi
}
