#### Helper functions for building/pulling Docker environments for viash


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
  docker pull $1
}

# ViashDockerPullElseBuild: pull a Docker image, else build it
#
# $1                  : image identifier with format `[registry/]image[:tag]`
# examples:
#   ViashDockerPullElseBuild mynewcomponent
function ViashDockerPullElseBuild {
  ViashDockerPull $1
  if [ $? -ne 0 ]; then
    ViashDockerBuild $1
  fi
}

# ViashDockerPullElseBuild: pull a Docker image, else build it
#
# $1                  : image identifier with format `[registry/]image[:tag]`
# ViashDockerBuild    : a Bash function which builds a docker image, takes image identifier as argument.
# examples:
#   ViashDockerPullElseBuild mynewcomponent alwaysbuild
function ViashDockerSetup {
  VSH_DOCKER_ID="$1"
  VSH_DOCKER_STRATEGY="$2"
  if [ "$VSH_DOCKER_STRATEGY" == "alwaysbuild" ]; then
    ViashDockerBuild $VSH_DOCKER_ID
  elif [ "$VSH_DOCKER_STRATEGY" == "alwayspull" ]; then
    ViashDockerPull $VSH_DOCKER_ID
  elif [ "$VSH_DOCKER_STRATEGY" == "alwayspullelsebuild" ]; then
    ViashDockerPullElseBuild $VSH_DOCKER_ID
  elif [ "$VSH_DOCKER_STRATEGY" == "dontbother" ]; then
    :
  elif [[ "$VSH_DOCKER_STRATEGY" =~ ^ifneedbe ]]; then
    ViashDockerLocalTagCheck $VSH_DOCKER_ID
    if [ $? -eq 0 ]; then
      echo "Image $VSH_DOCKER_ID already exists"
    elif [ "$VSH_DOCKER_STRATEGY" == "ifneedbebuild" ]; then
      ViashDockerBuild $VSH_DOCKER_ID
    elif [ "$VSH_DOCKER_STRATEGY" == "ifneedbepull" ]; then
      ViashDockerPull $VSH_DOCKER_ID
    elif [ "$VSH_DOCKER_STRATEGY" == "ifneedbepullelsebuild" ]; then
      ViashDockerPullElseBuild $VSH_DOCKER_ID
    else
      echo "Unrecognised Docker strategy: $VSH_DOCKER_STRATEGY"
    fi
  else
    echo "Unrecognised Docker strategy: $VSH_DOCKER_STRATEGY"
  fi
}
