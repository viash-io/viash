
# ViashRenderPythonValue 'string' "false" "${VIASH_META_NAME[@]:-@@VIASH_UNDEFINED@@}"
function ViashRenderPythonValue {
  local type="$1"
  local multiple="$2"
  shift 2

  if [ $# -eq 1 ] && [ "$1" == "@@VIASH_UNDEFINED@@" ]; then
    echo "None"
    return
  fi

  local out=""

  if [ "$multiple" == "true" ]; then
    out+="["
  fi
  while [ $# -gt 0 ]; do
    if [ "$1" == "@@VIASH_UNDEFINED_ITEM@@" ]; then
      out+="None"
    elif [ "$type" == "string" ] || [ "$type" == "file" ]; then
      # render it as r'...' but escape single quotes
      out+=$(echo "$1" | sed "s/'/\\\'/g" | sed "s/^/r\'/;s/$/\'/")
    elif [ "$type" == "boolean" ] || [ "$type" == "boolean_true" ] || [ "$type" == "boolean_false" ]; then
      out+="$1"
    else
      out+="$1"
    fi
    if [ "$multiple" == "true" ] && [ $# -gt 1 ]; then
      out+=", "
    fi
    shift
  done
  if [ "$multiple" == "true" ]; then
    out+="]"
  fi
  echo "$out"
}
