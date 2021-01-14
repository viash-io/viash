######## Helper functions for setting up Docker images for viash ########


# ViashDockerRemoteTagCheck: check whether a Docker image is available 
# on a remote. Assumes `docker login` has been performed, if relevant.
#
# $1                  : image identifier with format `[registry/]image[:tag]`
# exit code $?        : whether or not the image was found
# examples:
#   ViashDockerRemoteTagCheck python:latest
#   echo $?                                     # returns '0'
#   ViashDockerRemoteTagCheck sdaizudceahifu
#   echo $?                                     # returns '1'
function ViashDockerRemoteTagCheck {
  docker manifest inspect $1 > /dev/null 2> /dev/null
}

# ViashDockerLocalTagCheck: check whether a Docker image is available locally
#
# $1                  : image identifier with format `[registry/]image[:tag]`
# exit code $?        : whether or not the image was found
# examples:
#   docker pull python:latest
#   ViashDockerLocalTagCheck python:latest
#   echo $?                                     # returns '0'
#   ViashDockerLocalTagCheck sdaizudceahifu
#   echo $?                                     # returns '1'
function ViashDockerLocalTagCheck {
  [ -n "$(docker images -q $1)" ]
}

# ViashDockerPull: pull a Docker image
#
# $1                  : image identifier with format `[registry/]image[:tag]`
# exit code $?        : whether or not the image was found
# examples:
#   ViashDockerPull python:latest
#   echo $?                                     # returns '0'
#   ViashDockerPull sdaizudceahifu
#   echo $?                                     # returns '1'
function ViashDockerPull {
  docker pull $1 && return 0 || return 1
}

# ViashDockerPullElseBuild: pull a Docker image, else build it
#
# $1                  : image identifier with format `[registry/]image[:tag]`
# examples:
#   ViashDockerPullElseBuild mynewcomponent
function ViashDockerPullElseBuild {
  set +e
  ViashDockerPull $1
  out=$?
  set -e
  if [ $out -ne 0 ]; then
    ViashDockerBuild $@
  fi
}

# ViashDockerSetup: create a Docker image, according to specified docker setup strategy
#
# $1                  : image identifier with format `[registry/]image[:tag]`
# $2                  : docker setup strategy, see DockerSetupStrategy.scala
# ViashDockerBuild    : a Bash function which builds a docker image, takes image identifier as argument.
# examples:
#   ViashDockerPullElseBuild mynewcomponent alwaysbuild
function ViashDockerSetup {
  VSHD_ID="$1"
  VSHD_STRAT="$2"
  if [ "$VSHD_STRAT" == "alwaysbuild" -o "$VSHD_STRAT" == "build" ]; then
    ViashDockerBuild $VSHD_ID --no-cache
  elif [ "$VSHD_STRAT" == "alwayspull" -o "$VSHD_STRAT" == "pull" ]; then
    ViashDockerPull $VSHD_ID
  elif [ "$VSHD_STRAT" == "alwayspullelsebuild" -o "$VSHD_STRAT" == "pullelsebuild" ]; then
    ViashDockerPullElseBuild $VSHD_ID --no-cache
  elif [ "$VSHD_STRAT" == "alwayspullelsecachedbuild" -o "$VSHD_STRAT" == "pullelsecachedbuild" ]; then
    ViashDockerPullElseBuild $VSHD_ID
  elif [ "$VSHD_STRAT" == "alwayscachedbuild" -o "$VSHD_STRAT" == "cachedbuild" ]; then
    ViashDockerBuild $VSHD_ID
  elif [ "$VSHD_STRAT" == "donothing" -o "$VSHD_STRAT" == "meh" ]; then
    echo "Skipping setup."
  elif [[ "$VSHD_STRAT" =~ ^ifneedbe ]]; then
    ViashDockerLocalTagCheck $VSHD_ID
    if [ $? -eq 0 ]; then
      echo "Image $VSHD_ID already exists"
    elif [ "$VSHD_STRAT" == "ifneedbebuild" ]; then
      ViashDockerBuild $VSHD_ID --no-cache
    elif [ "$VSHD_STRAT" == "ifneedbecachedbuild" ]; then
      ViashDockerBuild $VSHD_ID
    elif [ "$VSHD_STRAT" == "ifneedbepull" ]; then
      ViashDockerPull $VSHD_ID
    elif [ "$VSHD_STRAT" == "ifneedbepullelsebuild" ]; then
      ViashDockerPullElseBuild $VSHD_ID --no-cache
    elif [ "$VSHD_STRAT" == "ifneedbepullelsecachedbuild" ]; then
      ViashDockerPullElseBuild $VSHD_ID
    else
      echo "Unrecognised Docker strategy: $VSHD_STRAT"
    fi
  else
    echo "Unrecognised Docker strategy: $VSHD_STRAT"
  fi
}

######## End of helper functions for setting up Docker images for viash ########
