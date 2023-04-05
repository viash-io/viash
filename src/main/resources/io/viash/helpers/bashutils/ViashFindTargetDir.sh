# ViashFindTargetDir: return the path of the '.build.yaml' file, following symlinks
# usage   : ViashFindTargetDir 'ScriptPath'
# $1      : The location from where to start the upward search
# returns : The absolute path of the '.build.yaml' file
function ViashFindTargetDir {
  SOURCE="$1"
  while [[ "$SOURCE" != "" && ! -e "$SOURCE/.build.yaml" ]]; do
    SOURCE=${SOURCE%/*}
  done
  echo $SOURCE
}
