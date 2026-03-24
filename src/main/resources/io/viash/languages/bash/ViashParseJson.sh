#!/usr/bin/env bash

# ViashParseJsonBash: Parse JSON parameters into Bash variables using jq
#
# Reads JSON from stdin and sets variables for each key-value pair.
#
# Structure:
#   - Top-level keys mapping to objects are treated as sections,
#     with variables named "section_key" (e.g., par_input, meta_name).
#   - Top-level scalar/array keys are set directly.
#   - Arrays become Bash arrays.
#   - Deep nesting (depth 3+) is stored as JSON strings.
#   - Null values leave the variable unset.
#
# Requires: jq (https://jqlang.github.io/jq/)
#
# Usage:
#   ViashParseJsonBash < json_file
#   ViashParseJsonBash <<< "$json_content"

function ViashParseJsonBash {
  local _viash_json
  _viash_json="$(cat)"

  if [ -z "$_viash_json" ]; then
    return 0
  fi

  # Verify jq is available
  if ! command -v jq &>/dev/null; then
    echo "ViashParseJsonBash: jq is required but not found. Install jq or set 'use_jq: false' in your bash_script resource." >&2
    return 1
  fi

  local _viash_assignments
  _viash_assignments="$(printf '%s' "$_viash_json" | jq -r '
    # Generate a bash assignment for a variable.
    # For arrays: varname=(elem1 elem2 ...)
    # For scalars: varname=value
    # For null: no output (variable left unset)
    def to_bash_assignment(varname):
      if type == "null" then
        empty
      elif type == "array" then
        varname + "=(" + ([.[] |
          if type == "null" then "\"null\""
          elif type == "boolean" or type == "number" then tostring | @sh
          elif type == "string" then @sh
          elif type == "object" or type == "array" then tojson | @sh
          else tojson | @sh
          end
        ] | join(" ")) + ")"
      elif type == "boolean" or type == "number" then
        varname + "=" + tostring
      elif type == "string" then
        varname + "=" + @sh
      elif type == "object" then
        varname + "=" + (tojson | @sh)
      else
        varname + "=" + (tojson | @sh)
      end;

    to_entries[] |
    if .value | type == "object" then
      # Section: iterate keys and produce section_key assignments
      .key as $section |
      .value | to_entries[] |
      .key as $key |
      .value | to_bash_assignment("\($section)_\($key)")
    else
      # Direct top-level key
      .key as $key |
      .value | to_bash_assignment($key)
    end
  ')" || {
    echo "ViashParseJsonBash: jq failed to parse JSON input" >&2
    return 1
  }

  if [ -n "$_viash_assignments" ]; then
    eval "$_viash_assignments"
  fi
}
