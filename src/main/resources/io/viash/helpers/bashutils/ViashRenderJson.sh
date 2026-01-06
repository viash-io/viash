# ViashRenderJsonKeyValue: renders a key-value pair in JSON format
#
# Arguments:
# $1: The key
# $2: The type of the value (string, boolean, boolean_true, boolean_false, file, double, integer)
# $3: Whether the value can be passed multiple times (true/false)
# $4+: The value(s) of the key
# return: prints the key-value pair in JSON format
#
# Examples:
#
# ViashRenderJsonKeyValue "input" "string" "false" "file.txt"
# ViashRenderJsonKeyValue "input" "string" "true" "file1.txt" "file2.txt"
function ViashRenderJsonKeyValue {
  local key="$1"
  local type="$2"
  local multiple="$3"
  shift 3

  local out="    \"${key}\":"

  # Handle null case
  if [ $# -eq 1 ] && [ "$1" == "@@VIASH_UNDEFINED@@" ]; then
    out+=" null"
    echo "$out"
    return
  fi

  # Handle multiple values (array)
  if [ "$multiple" == "true" ]; then
    out+=" ["
    local first=true
    while [ $# -gt 0 ]; do
      if [ "$first" == "false" ]; then
        out+=","
      fi
      first=false
      
      if [ "$1" == "@@VIASH_UNDEFINED_ITEM@@" ]; then
        out+=" null"
      elif [ "$type" == "string" ] || [ "$type" == "file" ]; then
        out+=" $(ViashRenderJsonQuotedValue "$key" "$1")"
      elif [ "$type" == "boolean" ] || [ "$type" == "boolean_true" ] || [ "$type" == "boolean_false" ]; then
        out+=" $(ViashRenderJsonBooleanValue "$key" "$1")"
      else
        out+=" $(ViashRenderJsonUnquotedValue "$key" "$1")"
      fi
      shift
    done
    out+=" ]"
  else
    # Handle single value
    out+=" "
    if [ "$1" == "@@VIASH_UNDEFINED_ITEM@@" ]; then
      out+="null"
    elif [ "$type" == "string" ] || [ "$type" == "file" ]; then
      out+="$(ViashRenderJsonQuotedValue "$key" "$1")"
    elif [ "$type" == "boolean" ] || [ "$type" == "boolean_true" ] || [ "$type" == "boolean_false" ]; then
      out+="$(ViashRenderJsonBooleanValue "$key" "$1")"
    else
      out+="$(ViashRenderJsonUnquotedValue "$key" "$1")"
    fi
  fi

  echo "$out"
}

function ViashRenderJsonQuotedValue {
  local key="$1"
  local value="$2"
  # escape backslashes, quotes, and newlines for JSON
  echo "$value" | \
    sed 's#\\#\\\\#g' | \
    sed 's#"#\\"#g' | \
    sed ':a;N;$!ba;s/\n/\\n/g' | \
    sed 's#^#"#g;s#$#"#g'
}

function ViashRenderJsonBooleanValue {
  local key="$1"
  local value="$2"
  # convert to lowercase
  value=$(echo "$value" | tr '[:upper:]' '[:lower:]')
  if [[ "$value" == "true" || "$value" == "yes" ]]; then
    echo "true"
  elif [[ "$value" == "false" || "$value" == "no" ]]; then
    echo "false"
  else
    echo "Argument '$key' has to be a boolean, but got '$value'. Use '--help' to get more information on the parameters."
  fi
}

function ViashRenderJsonUnquotedValue {
  local key="$1"
  local value="$2"
  echo "$value"
}
