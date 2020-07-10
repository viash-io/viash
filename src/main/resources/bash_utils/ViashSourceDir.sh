# ViashSourceDir: return the path of a bash file, following symlinks
# usage   : ViashSourceDir ${BASH_SOURCE[0]}
# $1      : Should always be set to ${BASH_SOURCE[0]}
# returns : The absolute path of the bash file
function ViashSourceDir {
  SOURCE="$1"
  while [ -h "$SOURCE" ]; do
    DIR="$( cd -P "$( dirname "$SOURCE" )" >/dev/null 2>&1 && pwd )"
    SOURCE="$(readlink "$SOURCE")"
    [[ $SOURCE != /* ]] && SOURCE="$DIR/$SOURCE"
  done
  cd -P "$( dirname "$SOURCE" )" >/dev/null 2>&1 && pwd
}
