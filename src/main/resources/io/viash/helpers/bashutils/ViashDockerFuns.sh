######## Helper functions for setting up Docker images for viash ########
# expects: ViashDockerBuild

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

# ViashDockerPush: push a Docker image
#
# $1                  : image identifier with format `[registry/]image[:tag]`
# exit code $?        : whether or not the image was found
# examples:
#   ViashDockerPush python:latest
#   echo $?                                     # returns '0'
#   ViashDockerPush sdaizudceahifu
#   echo $?                                     # returns '1'
function ViashDockerPush {
  ViashNotice "Pushing image to '$1'"
  save=$-; set +e
  if [ $VIASH_VERBOSITY -ge $VIASH_LOGCODE_INFO ]; then
    docker push $1
    out=$?
  else
    docker push $1 2> /dev/null > /dev/null
    out=$?
  fi
  [[ $save =~ e ]] && set -e
  if [ $out -eq 0 ]; then
    ViashNotice "Container '$1' push succeeded."
  else
    ViashError "Container '$1' push errored. You might not be logged in or have the necessary permissions."
  fi
  return $out
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
# $1          : image identifier with format `[registry/]image[:tag]`
# $2          : docker setup strategy, see DockerSetupStrategy.scala
# examples:
#   ViashDockerSetup mynewcomponent alwaysbuild
function ViashDockerSetup {
  local image_id="$1"
  local setup_strategy="$2"
  if [ "$setup_strategy" == "alwaysbuild" -o "$setup_strategy" == "build" -o "$setup_strategy" == "b" ]; then
    ViashDockerBuild $image_id --no-cache $(ViashDockerBuildArgs "$engine_id")
  elif [ "$setup_strategy" == "alwayspull" -o "$setup_strategy" == "pull" -o "$setup_strategy" == "p" ]; then
    ViashDockerPull $image_id
  elif [ "$setup_strategy" == "alwayspullelsebuild" -o "$setup_strategy" == "pullelsebuild" ]; then
    ViashDockerPullElseBuild $image_id --no-cache $(ViashDockerBuildArgs "$engine_id")
  elif [ "$setup_strategy" == "alwayspullelsecachedbuild" -o "$setup_strategy" == "pullelsecachedbuild" ]; then
    ViashDockerPullElseBuild $image_id $(ViashDockerBuildArgs "$engine_id")
  elif [ "$setup_strategy" == "alwayscachedbuild" -o "$setup_strategy" == "cachedbuild" -o "$setup_strategy" == "cb" ]; then
    ViashDockerBuild $image_id $(ViashDockerBuildArgs "$engine_id")
  elif [[ "$setup_strategy" =~ ^ifneedbe ]]; then
    local save=$-; set +e
    ViashDockerLocalTagCheck $image_id
    local outCheck=$?
    [[ $save =~ e ]] && set -e
    if [ $outCheck -eq 0 ]; then
      ViashInfo "Image $image_id already exists"
    elif [ "$setup_strategy" == "ifneedbebuild" ]; then
      ViashDockerBuild $image_id --no-cache $(ViashDockerBuildArgs "$engine_id")
    elif [ "$setup_strategy" == "ifneedbecachedbuild" ]; then
      ViashDockerBuild $image_id $(ViashDockerBuildArgs "$engine_id")
    elif [ "$setup_strategy" == "ifneedbepull" ]; then
      ViashDockerPull $image_id
    elif [ "$setup_strategy" == "ifneedbepullelsebuild" ]; then
      ViashDockerPullElseBuild $image_id --no-cache $(ViashDockerBuildArgs "$engine_id")
    elif [ "$setup_strategy" == "ifneedbepullelsecachedbuild" ]; then
      ViashDockerPullElseBuild $image_id $(ViashDockerBuildArgs "$engine_id")
    else
      ViashError "Unrecognised Docker strategy: $setup_strategy"
      exit 1
    fi
  elif [ "$setup_strategy" == "push" -o "$setup_strategy" == "forcepush" -o "$setup_strategy" == "alwayspush" ]; then
    ViashDockerPush "$image_id"
  elif [ "$setup_strategy" == "pushifnotpresent" -o "$setup_strategy" == "gentlepush" -o "$setup_strategy" == "maybepush" ]; then
    local save=$-; set +e
    ViashDockerRemoteTagCheck $image_id
    local outCheck=$?
    [[ $save =~ e ]] && set -e
    if [ $outCheck -eq 0 ]; then
      ViashNotice "Container '$image_id' exists, doing nothing."
    else
      ViashNotice "Container '$image_id' does not yet exist."
      ViashDockerPush "$image_id"
    fi
  elif [ "$setup_strategy" == "donothing" -o "$setup_strategy" == "meh" ]; then
    ViashNotice "Skipping setup."
  else
    ViashError "Unrecognised Docker strategy: $setup_strategy"
    exit 1
  fi
}

# ViashDockerCheckCommands: Check whether a docker container has the required commands
#
# $1                  : image identifier with format `[registry/]image[:tag]`
# $@                  : commands to verify being present
# examples:
#   ViashDockerCheckCommands bash:4.0 bash ps foo
function ViashDockerCheckCommands {
  local image_id="$1"
  shift 1
  local commands="$@"
  local save=$-; set +e
  local missing
  missing=$(docker run --rm --entrypoint=sh "$image_id" -c "for command in $commands; do command -v \$command >/dev/null 2>&1; if [ \$? -ne 0 ]; then echo \$command; exit 1; fi; done")
  local outCheck=$?
  [[ $save =~ e ]] && set -e
  if [ $outCheck -ne 0 ]; then
  	ViashError "Docker container '$image_id' does not contain command '$missing'."
  	exit 1
  fi
}

# ViashDockerBuild: build a docker image
# $1                               : image identifier with format `[registry/]image[:tag]`
# $...                             : additional arguments to pass to docker build
# $VIASH_META_TEMP_DIR             : temporary directory to store dockerfile & optional resources in
# $VIASH_META_FUNCTIONALITY_NAME   : name of the component
# $VIASH_META_RESOURCES_DIR        : directory containing the resources
# $VIASH_VERBOSITY                 : verbosity level
# exit code $?                     : whether or not the image was built successfully
function ViashDockerBuild {
  local image_id="$1"
  shift 1

  # create temporary directory to store dockerfile & optional resources in
  local tmpdir=$(mktemp -d "$VIASH_META_TEMP_DIR/dockerbuild-$VIASH_META_FUNCTIONALITY_NAME-XXXXXX")
  local dockerfile="$tmpdir/Dockerfile"
  function clean_up {
    rm -rf "$tmpdir"
  }
  trap clean_up EXIT

  # store dockerfile and resources
  ViashDockerfile "$VIASH_ENGINE_ID" > "$dockerfile"

  # generate the build command
  local docker_build_cmd="docker build -t '$image_id' $@ '$VIASH_META_RESOURCES_DIR' -f '$dockerfile'"

  # build the container
  ViashNotice "Building container '$image_id' with Dockerfile"
  ViashInfo "$docker_build_cmd"
  local save=$-; set +e
  if [ $VIASH_VERBOSITY -ge $VIASH_LOGCODE_INFO ]; then
    eval $docker_build_cmd
  else
    eval $docker_build_cmd &> "$tmpdir/docker_build.log"
  fi

  # check exit code
  local out=$?
  [[ $save =~ e ]] && set -e
  if [ $out -ne 0 ]; then
    ViashError "Error occurred while building container '$image_id'"
    if [ $VIASH_VERBOSITY -lt $VIASH_LOGCODE_INFO ]; then
      ViashError "Transcript: --------------------------------"
      cat "$tmpdir/docker_build.log"
      ViashError "End of transcript --------------------------"
    fi
    exit 1
  fi
}

######## End of helper functions for setting up Docker images for viash ########
