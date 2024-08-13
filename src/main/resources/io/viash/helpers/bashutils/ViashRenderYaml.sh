

function ViashRenderYamlQuotedValue {
  local key="$1"
  local value="$2"
  if [ "$value" == "UNDEFINED_ITEM" ]; then
    echo "null"
    return
  fi
  # escape quotes, backslashes and newlines, and then surround by quotes
  echo "$value" | sed 's#"#\\"#g' | sed 's#\\#\\\\#g' | sed ':a;N;$!ba;s/\n/\\n/g' | sed 's#^#"#g' | sed 's#$#"#g'
}

function ViashRenderYamlBooleanValue {
  local key="$1"
  local value="$2"
  if [ "$value" == "UNDEFINED_ITEM" ]; then
    echo "null"
    return
  fi
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
  if [ "$value" == "UNDEFINED_ITEM" ]; then
    echo "null"
    return
  fi
  echo "$value"
}

function ViashRenderYamlKeyValue {
  local key="$1"
  local type="$2"
  local multiple="$3"
  shift 3
  
  local out="$key: "

  if [ "$1" == "UNDEFINED" ]; then
    out+="null"
    echo $out
    return
  fi

  if [ "$multiple" == "true" ]; then
    out+="["
  fi

  first_elem=1

  for value in "$@"; do
    if [ $first_elem -eq 1 ]; then
      first_elem=0
    else
      out+=", "
    fi
    if [[ "$type" == "string" || "$type" == "file" ]]; then
      out+="$(ViashRenderYamlQuotedValue "$key" "$value")"
    elif [[ "$type" == "boolean" || "$type" == "boolean_true" || "$type" == "boolean_false" ]]; then
      out+="$(ViashRenderYamlBooleanValue "$key" "$value")"
    else
      out+="$(ViashRenderYamlUnquotedValue "$key" "$value")"
    fi
  done

  if [ "$multiple" == "true" ]; then
    out+="]"
  fi

  echo "$out"
}
