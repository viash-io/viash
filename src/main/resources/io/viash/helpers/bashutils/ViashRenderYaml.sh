# ViashRenderYamlKeyValue: renders a key-value pair in YAML format
#
# Arguments:
# $1: The key
# $2: The type of the value (string, boolean, boolean_true, boolean_false, file, double, integer)
# $3: Whether the value can be passed multiple times (true/false)
# $4+: The value(s) of the key
# return: prints the key-value pair in YAML format
#
# Examples:
#
# ViashRenderYamlKeyValue "input" "string" "false" "file.txt"
# ViashRenderYamlKeyValue "input" "string" "true" "file1.txt" "file2.txt"
function ViashRenderYamlKeyValue {
  local key="$1"
  local type="$2"
  local multiple="$3"
  shift 3

  local out="  ${key}:"

  if [ $# -eq 1 ] && [ "$1" == "@@VIASH_UNDEFINED@@" ]; then
    out+=" null"
    echo "$out"
    return
  fi

  while [ $# -gt 0 ]; do
    if [ "$multiple" == "true" ]; then
      out+=$'\n    - '
    else
      out+=" "
    fi
    if [ "$1" == "@@VIASH_UNDEFINED_ITEM@@" ]; then
      out+="null"
    elif [ "$type" == "string" ] || [ "$type" == "file" ]; then
      out+="$(ViashRenderYamlQuotedValue "$key" "$1")"
    elif [ "$type" == "boolean" ] || [ "$type" == "boolean_true" ] || [ "$type" == "boolean_false" ]; then
      out+="$(ViashRenderYamlBooleanValue "$key" "$1")"
    else
      out+="$(ViashRenderYamlUnquotedValue "$key" "$1")"
    fi
    shift
  done

  echo "$out"
}

function ViashRenderYamlQuotedValue {
  local key="$1"
  local value="$2"
  # escape quotes, backslashes and newlines, and then surround by quotes
  echo "$value" | \
    sed 's#\\#\\\\#g' | \
    sed 's#"#\\"#g' | \
    sed ':a;N;$!ba;s/\n/\\n/g' | \
    sed 's#^#"#g;s#$#"#g'
}

function ViashRenderYamlBooleanValue {
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

# can be infinite too
function ViashRenderYamlUnquotedValue {
  local key="$1"
  local value="$2"
  echo "$value"
}
