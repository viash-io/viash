# ViashFindTargetDir: return the path of the '.build.yaml' file, following symlinks
# usage   : ViashFindTargetDir 'ScriptPath'
# $1      : The location from where to start the upward search
# returns : The absolute path of the '.build.yaml' file
function ViashFindTargetDir {
  local source="$1"
  while [[ "$source" != "" && ! -e "$source/.build.yaml" ]]; do
    source=${source%/*}
  done
  echo $source
}
