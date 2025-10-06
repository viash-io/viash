# ViashParseYamlBash: Parse simple YAML into bash environment variables
#
# This function reads YAML content and converts it into bash environment variables.
# Arrays are converted to bash arrays.
# 
# Usage: ViashParseYamlBash < yaml_file
# or: ViashParseYamlBash <<< "$yaml_content"
#
# The YAML format expected is simple:
#   section1:
#     key: value
#     array_key:
#       - item1
#       - item2
#   section2:
#     nested_key:
#       subkey: value
#
# Output: Sets environment variables that can be accessed in bash.
# 
# Example output:
# section1_key="value"
# section1_array_key=(item1 item2)
# section2_nested_key_subkey="value"
function ViashParseYamlBash() {
  local default_prefix="${1:-VIASH_PAR_}"
  local line
  local key
  local value
  local array_key=""
  local in_array=false
  local current_section=""
  local current_prefix="$default_prefix"
  
  while IFS= read -r line; do
    # Skip empty lines and comments
    if [[ -z "$line" || "$line" =~ ^[[:space:]]*# ]]; then
      continue
    fi
    
    # Check for top-level sections (any section name followed by colon)
    if [[ "$line" =~ ^([a-zA-Z_][a-zA-Z0-9_]*):[[:space:]]*$ ]]; then
      current_section="${BASH_REMATCH[1]}"
      current_prefix="${current_section}_"
      # Reset array state when entering new section
      in_array=false
      array_key=""
      continue
    fi
    
    # Check if this is an array item
    if [[ "$line" =~ ^[[:space:]]*-[[:space:]]*(.*) ]]; then
      if [[ "$in_array" == true && -n "$array_key" ]]; then
        local item="${BASH_REMATCH[1]}"
        # Remove quotes if present
        if [[ "$item" =~ ^\"(.*)\"$ ]]; then
          item="${BASH_REMATCH[1]}"
          # Unescape quotes and newlines
          item="${item//\\\"/\"}"
          item="${item//\\n/$'\n'}"
          item="${item//\\\\/\\}"
        elif [[ "$item" == "null" ]]; then
          item=""
        fi
        
        # Append to array
        if [[ -v "${current_prefix}${array_key}" ]]; then
          eval "declare -g -a ${current_prefix}${array_key}+=('$item')"
        else
          eval "declare -g -a ${current_prefix}${array_key}=('$item')"
        fi
      fi
      continue
    fi
    
    # Check for key-value pairs
    if [[ "$line" =~ ^[[:space:]]*([^:]+):[[:space:]]*(.*) ]]; then
      key="${BASH_REMATCH[1]}"
      value="${BASH_REMATCH[2]}"
      
      # Clean up key (remove leading/trailing spaces)
      key="${key//[[:space:]]/}"
      
      # Reset array state
      in_array=false
      array_key=""
      
      if [[ -z "$value" ]]; then
        # This might be the start of an array
        in_array=true
        array_key="$key"
        continue
      elif [[ "$value" == "null" ]]; then
        eval "${current_prefix}${key}=''"
      elif [[ "$value" =~ ^\"(.*)\"$ ]]; then
        # Quoted string - unescape
        local unquoted="${BASH_REMATCH[1]}"
        unquoted="${unquoted//\\\"/\"}"
        unquoted="${unquoted//\\n/$'\n'}"
        unquoted="${unquoted//\\\\/\\}"
        eval "${current_prefix}${key}='$unquoted'"
      elif [[ "$value" =~ ^(true|false)$ ]]; then
        # Boolean
        eval "${current_prefix}${key}='$value'"
      else
        # Unquoted value (number, etc.)
        eval "${current_prefix}${key}='$value'"
      fi
    fi
  done
}
