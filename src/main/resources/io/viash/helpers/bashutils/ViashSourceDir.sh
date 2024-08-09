# ViashSourceDir: return the path of a bash file, following symlinks
# usage   : ViashSourceDir ${BASH_SOURCE[0]}
# $1      : Should always be set to ${BASH_SOURCE[0]}
# returns : The absolute path of the bash file
function ViashSourceDir {
  local source="$1"
  while [ -h "$source" ]; do
    local dir="$( cd -P "$( dirname "$source" )" >/dev/null 2>&1 && pwd )"
    source="$(readlink "$source")"
    [[ $source != /* ]] && source="$dir/$source"
  done
  cd -P "$( dirname "$source" )" >/dev/null 2>&1 && pwd
}
