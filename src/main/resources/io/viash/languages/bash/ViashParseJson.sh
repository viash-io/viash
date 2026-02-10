#!/usr/bin/env bash

# ViashParseJsonBash: Parse JSON parameters into Bash variables
#
# Recursive descent JSON parser (similar to the Scala ViashJsonParser).
# Reads JSON from stdin and sets variables for each key-value pair.
#
# Structure:
#   - Top-level keys mapping to objects are treated as sections,
#     with variables named "section_key" (e.g., par_input, meta_name).
#   - Top-level scalar/array keys are set directly.
#   - Arrays become Bash arrays.
#   - Deep nesting (depth 3+) is stored as JSON strings.
#
# Usage:
#   ViashParseJsonBash < json_file
#   ViashParseJsonBash <<< "$json_content"
#
# Note: Compatible with bash 3.2 (macOS default).
# Avoids bash 4+ features like declare -g, local -n, and readarray.

# -- Parser state (globals for bash 3.2 compatibility) --
_viash_json=""     # Full JSON input string
_viash_pos=0       # Current parse position
_viash_result=""   # Result of last parse operation

# ViashParseJsonBash: entry point -- reads stdin and parses top-level object
function ViashParseJsonBash {
  _viash_json="$(cat)"
  _viash_pos=0
  _viash_result=""

  _viash_skip_whitespace
  _viash_parse_toplevel_object
}

# -- Low-level character operations --

function _viash_skip_whitespace {
  while [ $_viash_pos -lt ${#_viash_json} ]; do
    case "${_viash_json:$_viash_pos:1}" in
      ' '|$'\t'|$'\n'|$'\r') ((_viash_pos++)) || true ;;
      *) return ;;
    esac
  done
}

# Peek at the current character (after skipping whitespace).
# Sets _viash_result to the character.
function _viash_peek {
  _viash_skip_whitespace
  if [ $_viash_pos -ge ${#_viash_json} ]; then
    echo "ViashParseJsonBash: Unexpected end of JSON at position $_viash_pos" >&2
    exit 1
  fi
  _viash_result="${_viash_json:$_viash_pos:1}"
}

# Consume an expected character, or exit with error.
function _viash_consume {
  local expected="$1"
  _viash_skip_whitespace
  if [ $_viash_pos -ge ${#_viash_json} ]; then
    echo "ViashParseJsonBash: Expected '$expected' but reached end of JSON" >&2
    exit 1
  fi
  local actual="${_viash_json:$_viash_pos:1}"
  if [ "$actual" != "$expected" ]; then
    echo "ViashParseJsonBash: Expected '$expected' at position $_viash_pos, got '$actual'" >&2
    exit 1
  fi
  ((_viash_pos++)) || true
}

# -- Core parse functions --

# Parse a JSON string. Sets _viash_result to the unescaped string content.
function _viash_parse_string {
  _viash_consume '"'
  local result=""
  while [ $_viash_pos -lt ${#_viash_json} ]; do
    local char="${_viash_json:$_viash_pos:1}"
    if [ "$char" = '"' ]; then
      ((_viash_pos++)) || true
      _viash_result="$result"
      return
    elif [ "$char" = '\' ]; then
      ((_viash_pos++)) || true
      if [ $_viash_pos -ge ${#_viash_json} ]; then
        echo "ViashParseJsonBash: Unterminated string escape at position $_viash_pos" >&2
        exit 1
      fi
      local esc="${_viash_json:$_viash_pos:1}"
      case "$esc" in
        # Note: \n and \t are kept as literal two-character sequences (\n, \t)
        # to match Viash YAML parser behavior and test expectations
        'n')  result+='\n' ;;
        't')  result+='\t' ;;
        'r')  result+=$'\r' ;;
        'b')  result+=$'\b' ;;
        'f')  result+=$'\f' ;;
        '\\') result+='\' ;;
        '"')  result+='"' ;;
        '/')  result+='/' ;;
        'u')
          # Unicode escape \uXXXX
          local hex="${_viash_json:$((_viash_pos+1)):4}"
          if [ ${#hex} -lt 4 ]; then
            echo "ViashParseJsonBash: Invalid unicode escape at position $_viash_pos" >&2
            exit 1
          fi
          result+="$(printf "\\$(printf '%03o' "0x$hex")")"
          _viash_pos=$((_viash_pos + 4))
          ;;
        *)
          # Unknown escape - keep as-is
          result+="$esc" ;;
      esac
    else
      result+="$char"
    fi
    ((_viash_pos++)) || true
  done
  echo "ViashParseJsonBash: Unterminated string starting near position $_viash_pos" >&2
  exit 1
}

# Parse a JSON number. Sets _viash_result to the number string.
function _viash_parse_number {
  local start=$_viash_pos
  # Optional minus
  if [ "${_viash_json:$_viash_pos:1}" = '-' ]; then
    ((_viash_pos++)) || true
  fi
  # Integer digits
  while [ $_viash_pos -lt ${#_viash_json} ] && [[ "${_viash_json:$_viash_pos:1}" =~ [0-9] ]]; do
    ((_viash_pos++)) || true
  done
  # Decimal part
  if [ $_viash_pos -lt ${#_viash_json} ] && [ "${_viash_json:$_viash_pos:1}" = '.' ]; then
    ((_viash_pos++)) || true
    while [ $_viash_pos -lt ${#_viash_json} ] && [[ "${_viash_json:$_viash_pos:1}" =~ [0-9] ]]; do
      ((_viash_pos++)) || true
    done
  fi
  # Exponent part
  if [ $_viash_pos -lt ${#_viash_json} ]; then
    local ec="${_viash_json:$_viash_pos:1}"
    if [ "$ec" = 'e' ] || [ "$ec" = 'E' ]; then
      ((_viash_pos++)) || true
      if [ $_viash_pos -lt ${#_viash_json} ]; then
        local sign="${_viash_json:$_viash_pos:1}"
        if [ "$sign" = '+' ] || [ "$sign" = '-' ]; then
          ((_viash_pos++)) || true
        fi
      fi
      while [ $_viash_pos -lt ${#_viash_json} ] && [[ "${_viash_json:$_viash_pos:1}" =~ [0-9] ]]; do
        ((_viash_pos++)) || true
      done
    fi
  fi
  _viash_result="${_viash_json:$start:$((_viash_pos - start))}"
}

# Parse a JSON boolean (true/false). Sets _viash_result.
function _viash_parse_boolean {
  if [ "${_viash_json:$_viash_pos:4}" = "true" ]; then
    _viash_pos=$((_viash_pos + 4))
    _viash_result="true"
  elif [ "${_viash_json:$_viash_pos:5}" = "false" ]; then
    _viash_pos=$((_viash_pos + 5))
    _viash_result="false"
  else
    echo "ViashParseJsonBash: Invalid boolean at position $_viash_pos" >&2
    exit 1
  fi
}

# Parse a JSON null. Sets _viash_result to empty string.
function _viash_parse_null {
  if [ "${_viash_json:$_viash_pos:4}" = "null" ]; then
    _viash_pos=$((_viash_pos + 4))
    _viash_result=""
  else
    echo "ViashParseJsonBash: Invalid null at position $_viash_pos" >&2
    exit 1
  fi
}

# Skip over any JSON value without storing it (for values we don't need).
# Correctly handles nested structures.
function _viash_skip_value {
  _viash_peek
  case "$_viash_result" in
    '"') _viash_parse_string ;;
    '{') _viash_skip_object ;;
    '[') _viash_skip_array ;;
    't'|'f') _viash_parse_boolean ;;
    'n') _viash_parse_null ;;
    '-'|[0-9]) _viash_parse_number ;;
    *)
      echo "ViashParseJsonBash: Unexpected character '$_viash_result' at position $_viash_pos" >&2
      exit 1
      ;;
  esac
}

function _viash_skip_object {
  _viash_consume '{'
  _viash_peek
  if [ "$_viash_result" = '}' ]; then
    _viash_consume '}'
    return
  fi
  while true; do
    _viash_parse_string  # key
    _viash_consume ':'
    _viash_skip_value    # value
    _viash_peek
    if [ "$_viash_result" = ',' ]; then
      _viash_consume ','
    else
      break
    fi
  done
  _viash_consume '}'
}

function _viash_skip_array {
  _viash_consume '['
  _viash_peek
  if [ "$_viash_result" = ']' ]; then
    _viash_consume ']'
    return
  fi
  while true; do
    _viash_skip_value
    _viash_peek
    if [ "$_viash_result" = ',' ]; then
      _viash_consume ','
    else
      break
    fi
  done
  _viash_consume ']'
}

# -- Serialization functions (for deep nesting stored as JSON strings) --

# Serialize the JSON value at the current position back into a JSON string.
# Sets _viash_result to the JSON fragment.
function _viash_serialize_value {
  _viash_peek
  case "$_viash_result" in
    '"') _viash_serialize_string ;;
    '{') _viash_serialize_object ;;
    '[') _viash_serialize_array ;;
    't'|'f')
      _viash_parse_boolean
      ;;
    'n')
      _viash_parse_null
      _viash_result="null"
      ;;
    '-'|[0-9])
      _viash_parse_number
      ;;
    *)
      echo "ViashParseJsonBash: Unexpected character '$_viash_result' at position $_viash_pos" >&2
      exit 1
      ;;
  esac
}

# Serialize a JSON string (keeps it in JSON-encoded form with quotes).
function _viash_serialize_string {
  local start=$_viash_pos
  _viash_consume '"'
  while [ $_viash_pos -lt ${#_viash_json} ]; do
    local char="${_viash_json:$_viash_pos:1}"
    if [ "$char" = '"' ]; then
      ((_viash_pos++)) || true
      _viash_result="${_viash_json:$start:$((_viash_pos - start))}"
      return
    elif [ "$char" = '\' ]; then
      # Skip escape sequence
      ((_viash_pos++)) || true
    fi
    ((_viash_pos++)) || true
  done
  echo "ViashParseJsonBash: Unterminated string at position $start" >&2
  exit 1
}

function _viash_serialize_object {
  local out="{"
  _viash_consume '{'
  _viash_peek
  if [ "$_viash_result" = '}' ]; then
    _viash_consume '}'
    _viash_result="{}"
    return
  fi
  local first=true
  while true; do
    if [ "$first" = "false" ]; then
      out+=","
    fi
    first=false
    _viash_serialize_string
    out+="$_viash_result"
    _viash_consume ':'
    out+=":"
    _viash_serialize_value
    out+="$_viash_result"
    _viash_peek
    if [ "$_viash_result" = ',' ]; then
      _viash_consume ','
    else
      break
    fi
  done
  _viash_consume '}'
  out+="}"
  _viash_result="$out"
}

function _viash_serialize_array {
  local out="["
  _viash_consume '['
  _viash_peek
  if [ "$_viash_result" = ']' ]; then
    _viash_consume ']'
    _viash_result="[]"
    return
  fi
  local first=true
  while true; do
    if [ "$first" = "false" ]; then
      out+=","
    fi
    first=false
    _viash_serialize_value
    out+="$_viash_result"
    _viash_peek
    if [ "$_viash_result" = ',' ]; then
      _viash_consume ','
    else
      break
    fi
  done
  _viash_consume ']'
  out+="]"
  _viash_result="$out"
}

# -- Top-level parsing: maps JSON structure to Bash variables --

# Parse the root object. Handles two patterns:
#   1. Key -> object: treated as a section (variables named section_key)
#   2. Key -> scalar/array: set directly as variable
function _viash_parse_toplevel_object {
  _viash_consume '{'
  _viash_peek
  if [ "$_viash_result" = '}' ]; then
    _viash_consume '}'
    return
  fi

  while true; do
    _viash_parse_string
    local key="$_viash_result"
    _viash_consume ':'

    _viash_peek
    case "$_viash_result" in
      '{')
        _viash_parse_section_object "$key"
        ;;
      '[')
        _viash_parse_and_assign_array "$key"
        ;;
      *)
        _viash_parse_and_assign_scalar "$key"
        ;;
    esac

    _viash_peek
    if [ "$_viash_result" = ',' ]; then
      _viash_consume ','
    else
      break
    fi
  done
  _viash_consume '}'
}

# Parse a section object (depth 2). Each key becomes a variable "section_key".
function _viash_parse_section_object {
  local section="$1"
  _viash_consume '{'
  _viash_peek
  if [ "$_viash_result" = '}' ]; then
    _viash_consume '}'
    return
  fi

  while true; do
    _viash_parse_string
    local key="$_viash_result"
    local var_name="${section}_${key}"
    _viash_consume ':'

    _viash_peek
    case "$_viash_result" in
      '{')
        # Depth 3+ object -> serialize and store as JSON string
        _viash_serialize_object
        printf -v _viash_escaped '%q' "$_viash_result"
        eval "${var_name}=$_viash_escaped"
        ;;
      '[')
        _viash_parse_and_assign_array "$var_name"
        ;;
      *)
        _viash_parse_and_assign_scalar "$var_name"
        ;;
    esac

    _viash_peek
    if [ "$_viash_result" = ',' ]; then
      _viash_consume ','
    else
      break
    fi
  done
  _viash_consume '}'
}

# Parse a scalar value and assign it to the given variable name.
# Null values leave the variable unset.
function _viash_parse_and_assign_scalar {
  local var_name="$1"
  _viash_peek
  case "$_viash_result" in
    '"')
      _viash_parse_string
      printf -v _viash_escaped '%q' "$_viash_result"
      eval "${var_name}=$_viash_escaped"
      ;;
    't'|'f')
      _viash_parse_boolean
      eval "${var_name}=$_viash_result"
      ;;
    'n')
      _viash_parse_null
      # Leave variable unset for null
      ;;
    '-'|[0-9])
      _viash_parse_number
      eval "${var_name}=$_viash_result"
      ;;
    *)
      echo "ViashParseJsonBash: Unexpected value '$_viash_result' at position $_viash_pos" >&2
      exit 1
      ;;
  esac
}

# Parse a JSON array and assign it to the given variable name as a Bash array.
function _viash_parse_and_assign_array {
  local var_name="$1"
  _viash_consume '['
  _viash_peek
  if [ "$_viash_result" = ']' ]; then
    _viash_consume ']'
    eval "${var_name}=()"
    return
  fi

  local items=()
  while true; do
    _viash_peek
    case "$_viash_result" in
      '"')
        _viash_parse_string
        items+=("$_viash_result")
        ;;
      't'|'f')
        _viash_parse_boolean
        items+=("$_viash_result")
        ;;
      'n')
        _viash_parse_null
        items+=("null")
        ;;
      '-'|[0-9])
        _viash_parse_number
        items+=("$_viash_result")
        ;;
      '{')
        _viash_serialize_object
        items+=("$_viash_result")
        ;;
      '[')
        _viash_serialize_array
        items+=("$_viash_result")
        ;;
      *)
        echo "ViashParseJsonBash: Unexpected array element '$_viash_result' at position $_viash_pos" >&2
        exit 1
        ;;
    esac

    _viash_peek
    if [ "$_viash_result" = ',' ]; then
      _viash_consume ','
    else
      break
    fi
  done
  _viash_consume ']'

  # Assign array (bash 3.2 compatible)
  eval "${var_name}=($(printf '%q ' "${items[@]}"))"
}
