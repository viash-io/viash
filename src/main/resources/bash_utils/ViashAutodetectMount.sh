# ViashAutodetectMount: auto configuring docker mounts from parameters
# $1                  : The parameter value
# returns             : New parameter
# $VIASH_EXTRA_MOUNTS : Added another parameter to be passed to docker
# examples:
#   ViashAutodetectMount /path/to/bar
#   -> sets VIASH_DOCKER_MOUNTS to "-v /path/to:/viash_automount/path/to"
#   -> returns /viash_automount/path/to/bar
function ViashAutodetectMount {
  local abs_path=`ViashAbsolutePath $1`
  local mount_source
  local base_name
  if [ -d $abs_path ]; then
    mount_source=$abs_path
    base_name=""
  else
    mount_source=`dirname $abs_path`
    base_name=`base_name $abs_path`
  fi
  local mount_target="/viash_automount/$mount_source"
  local new_data="-v \"$mount_source:$mount_target\""
  if [ -z "$var" ]; then
    VIASH_DOCKER_MOUNTS=$new_data
  else
    VIASH_DOCKER_MOUNTS="$VIASH_DOCKER_MOUNTS $new_data"
  fi
  echo "\"$mount_target/$base_name\""
}
