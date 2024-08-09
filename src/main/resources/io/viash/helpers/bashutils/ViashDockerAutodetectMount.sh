# ViashDockerAutodetectMount: auto configuring docker mounts from parameters
# $1                  : The parameter value
# returns             : New parameter
# $VIASH_DIRECTORY_MOUNTS : Added another parameter to be passed to docker
# examples:
#   ViashDockerAutodetectMount /path/to/bar      # returns '/viash_automount/path/to/bar'
#   ViashDockerAutodetectMountArg /path/to/bar   # returns '--volume="/path/to:/viash_automount/path/to"'
function ViashDockerAutodetectMount {
  local abs_path=$(ViashAbsolutePath "$1")
  local mount_source
  local base_name
  if [ -d "$abs_path" ]; then
    mount_source="$abs_path"
    base_name=""
  else
    mount_source=`dirname "$abs_path"`
    base_name=`basename "$abs_path"`
  fi
  local mount_target="/viash_automount$mount_source"
  if [ -z "$base_name" ]; then
    echo "$mount_target"
  else
    echo "$mount_target/$base_name"
  fi
}
function ViashDockerAutodetectMountArg {
  local abs_path=$(ViashAbsolutePath "$1")
  local mount_source
  local base_name
  if [ -d "$abs_path" ]; then
    mount_source="$abs_path"
    base_name=""
  else
    mount_source=`dirname "$abs_path"`
    base_name=`basename "$abs_path"`
  fi
  local mount_target="/viash_automount$mount_source"
  ViashDebug "ViashDockerAutodetectMountArg $1 -> $mount_source -> $mount_target"
  echo "--volume=\"$mount_source:$mount_target\""
}
function ViashDockerStripAutomount {
  local abs_path=$(ViashAbsolutePath "$1")
  echo "${abs_path#/viash_automount}"
}