# ViashAutodetectMount: auto configuring docker mounts from parameters
# $1                  : The parameter name
# $2                  : The parameter value
# returns             : New parameter
# $VIASH_EXTRA_MOUNTS : Added another parameter to be passed to docker
# examples:
#   ViashAutodetectMount --foo /path/to/bar
#   -> sets VIASH_EXTRA_MOUNTS to "-v /path/to:/viash_automount/foo"
#   -> returns /viash_automount/foo/bar
function ViashAutodetectMount {
  local PARNAME=`echo $1 | sed 's#^-*##'`
  local ABSPATH=`ViashAbsolutePath $2`
  local NEWMOUNT
  local NEWNAME
  if [ -d $ABSPATH ]; then
    NEWMOUNT=$ABSPATH
    NEWNAME=""
  else
    NEWMOUNT=`dirname $ABSPATH`
    NEWNAME=`basename $ABSPATH`
  fi
  VIASH_EXTRA_MOUNTS="-v \"$NEWMOUNT:/viash_automount/$PARNAME\" $VIASH_EXTRA_MOUNTS"
  echo "\"/viash_automount/$PARNAME/$NEWNAME\""
}
