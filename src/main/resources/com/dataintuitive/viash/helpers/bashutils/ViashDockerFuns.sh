######## Helper functions for setting up Docker images for viash ########

# ViashDockerInstallationCheck: check whether Docker is installed correctly
#
# examples:
#   ViashDockerInstallationCheck
function ViashDockerInstallationCheck {
  ViashDebug "Checking whether Docker is installed"
  if [ ! command -v docker &> /dev/null ]; then
    ViashCritical "Docker doesn't seem to be installed. See 'https://docs.docker.com/get-docker/' for instructions."
    exit 1
  fi

  ViashDebug "Checking whether the Docker daemon is running"
  save=$-; set +e
  docker_version=$(docker version --format '{{.Client.APIVersion}}' 2> /dev/null)
  out=$?
  [[ $save =~ e ]] && set -e
  if [ $out -ne 0 ]; then
    ViashCritical "Docker daemon does not seem to be running. Try one of the following:"
    ViashCritical "- Try running 'dockerd' in the command line"
    ViashCritical "- See https://docs.docker.com/config/daemon/"
    exit 1
  fi
}

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
  ViashNotice "Checking if Docker image is available at '$1'"
  if [ $VIASH_VERBOSITY -ge $VIASH_LOGCODE_INFO ]; then
    docker pull $1 && return 0 || return 1
  else
    save=$-; set +e
    docker pull $1 2> /dev/null > /dev/null
    out=$?
    [[ $save =~ e ]] && set -e
    if [ $out -ne 0 ]; then
      ViashWarning "Could not pull from '$1'. Docker image doesn't exist or is not accessible."
    fi
    return $out
  fi
}

# ViashDockerPullElseBuild: pull a Docker image, else build it
#
# $1                  : image identifier with format `[registry/]image[:tag]`
# ViashDockerBuild    : a Bash function which builds a docker image, takes image identifier as argument.
# examples:
#   ViashDockerPullElseBuild mynewcomponent
function ViashDockerPullElseBuild {
  save=$-; set +e
  ViashDockerPull $1
  out=$?
  [[ $save =~ e ]] && set -e
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
#   ViashDockerSetup mynewcomponent alwaysbuild
function ViashDockerSetup {
  VSHD_ID="$1"
  VSHD_STRAT="$2"
  if [ "$VSHD_STRAT" == "alwaysbuild" -o "$VSHD_STRAT" == "build" -o "$VSHD_STRAT" == "b" ]; then
    ViashDockerBuild $VSHD_ID --no-cache
  elif [ "$VSHD_STRAT" == "alwayspull" -o "$VSHD_STRAT" == "pull" -o "$VSHD_STRAT" == "p" ]; then
    ViashDockerPull $VSHD_ID
  elif [ "$VSHD_STRAT" == "alwayspullelsebuild" -o "$VSHD_STRAT" == "pullelsebuild" ]; then
    ViashDockerPullElseBuild $VSHD_ID --no-cache
  elif [ "$VSHD_STRAT" == "alwayspullelsecachedbuild" -o "$VSHD_STRAT" == "pullelsecachedbuild" ]; then
    ViashDockerPullElseBuild $VSHD_ID
  elif [ "$VSHD_STRAT" == "alwayscachedbuild" -o "$VSHD_STRAT" == "cachedbuild" -o "$VSHD_STRAT" == "cb" ]; then
    ViashDockerBuild $VSHD_ID
  elif [[ "$VSHD_STRAT" =~ ^ifneedbe ]]; then
    save=$-; set +e
    ViashDockerLocalTagCheck $VSHD_ID
    outCheck=$?
    [[ $save =~ e ]] && set -e
    if [ $outCheck -eq 0 ]; then
      ViashInfo "Image $VSHD_ID already exists"
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
      ViashError "Unrecognised Docker strategy: $VSHD_STRAT"
      exit 1
    fi
  elif [ "$VSHD_STRAT" == "push" -o "$VSHD_STRAT" == "forcepush" -o "$VSHD_STRAT" == "alwayspush" ]; then
    save=$-; set +e
    docker push $VSHD_ID
    outPush=$?
    [[ $save =~ e ]] && set -e
    if [ $outPush -eq 0 ]; then
      ViashNotice "Container '$VSHD_ID' push succeeded."
    else
      ViashError "Container '$VSHD_ID' push errored."
      exit 1
    fi
  elif [ "$VSHD_STRAT" == "pushifnotpresent" -o "$VSHD_STRAT" == "gentlepush" -o "$VSHD_STRAT" == "maybepush" ]; then
    save=$-; set +e
    ViashDockerRemoteTagCheck $VSHD_ID
    outCheck=$?
    [[ $save =~ e ]] && set -e
    if [ $outCheck -eq 0 ]; then
      ViashNotice "Container '$VSHD_ID' exists, doing nothing."
    else
      ViashNotice "Container '$VSHD_ID' does not yet exist."
      save=$-; set +e
      docker push $1 > /dev/null 2> /dev/null
      outPush=$?
      [[ $save =~ e ]] && set -e
      if [ $outPush -eq 0 ]; then
        ViashNotice "Container '$VSHD_ID' push succeeded."
      else
      ViashError "Container '$VSHD_ID' push errored."
        exit 1
      fi
    fi
  elif [ "$VSHD_STRAT" == "donothing" -o "$VSHD_STRAT" == "meh" ]; then
    ViashNotice "Skipping setup."
  else
    ViashError "Unrecognised Docker strategy: $VSHD_STRAT"
    exit 1
  fi
}


######## End of helper functions for setting up Docker images for viash ########
