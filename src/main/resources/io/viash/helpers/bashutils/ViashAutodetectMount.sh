# ViashAutodetectMount: auto configuring docker mounts from parameters
# $1                  : The parameter value
# returns             : New parameter
# $VIASH_EXTRA_MOUNTS : Added another parameter to be passed to docker
# examples:
#   ViashAutodetectMount /path/to/bar      # returns '/viash_automount/path/to/bar'
#   ViashAutodetectMountArg /path/to/bar   # returns '--volume="/path/to:/viash_automount/path/to"'
function ViashAutodetectMount {
  abs_path=$(ViashAbsolutePath "$1")
  if [ -d "$abs_path" ]; then
    mount_source="$abs_path"
    base_name=""
  else
    mount_source=`dirname "$abs_path"`
    base_name=`basename "$abs_path"`
  fi
  mount_target="/viash_automount$mount_source"
  if [ -z "$base_name" ]; then
    echo "$mount_target"
  else
    echo "$mount_target/$base_name"
  fi
}
function ViashAutodetectMountArg {
  abs_path=$(ViashAbsolutePath "$1")
  if [ -d "$abs_path" ]; then
    mount_source="$abs_path"
    base_name=""
  else
    mount_source=`dirname "$abs_path"`
    base_name=`basename "$abs_path"`
  fi
  mount_target="/viash_automount$mount_source"
  ViashDebug "ViashAutodetectMountArg $1 -> $mount_source -> $mount_target"
  echo "--volume=\"$mount_source:$mount_target\""
}
function ViashStripAutomount {
  abs_path=$(ViashAbsolutePath "$1")
  echo "${abs_path#/viash_automount}"
}