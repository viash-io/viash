#!/bin/bash

# start creating command
command_builder=(
  ns build
  --src "$par_src"
  --parallel --write_meta
)

# check par mode
if [ "$par_mode" == "development" ]; then
  echo "In development mode with 'dev'."
elif [ "$par_mode" == "integration" ]; then
  echo "In integration mode with tag '$par_tag'."
elif [ "$par_mode" == "release" ]; then
  echo "In RELEASE mode with tag '$par_tag'."
else
  echo "Error: Not a valid mode argument '$par_mode'."
  exit 1
fi

# check tag
if [ "$par_mode" == "development" ]; then
  if [ ! -z "$par_tag" ]; then
    echo "Warning: '--tag' is ignored when '--mode=$par_mode'."
  fi
  par_tag="dev"
fi
if [ -z "$par_tag" ]; then
  echo "Error: --tag is a requirement argument when '--mode=$par_mode'."
  exit 1
fi

command_builder+=(
  --config_mod ".functionality.version := '$par_tag'"
)

# derive setup strategy
if [ "$par_mode" == "development" ]; then
  if [ "$par_no_cache" == "true" ]; then
    setup_strat="build"
  else
    setup_strat="cachedbuild"
  fi
elif [ "$par_mode" == "integration" ]; then
  echo "Warning: --par_no_cache is ignored when '--mode=$par_mode'."
  setup_strat="ifneedbepullelsecachedbuild"
elif [ "$par_mode" == "release" ]; then
  echo "Warning: --par_no_cache is ignored when '--mode=$par_mode'."
  setup_strat="build"
fi

command_builder+=(
  --setup "$setup_strat"
)

# check registry and organization
if [ "$par_mode" == "development" ]; then
  if [ ! -z "$par_registry" ]; then
    [[ "$par_verbose" == "true" ]] && echo "Note: --par_registry is ignored when '--mode=development'."
    unset par_registry
  fi

  if [ ! -z "$par_organization" ]; then
    [[ "$par_verbose" == "true" ]] && echo "Note: --par_organization is ignored when '--mode=development'."
    unset par_organization
  fi
fi

################ COMMON PARAMS ################

# check viash arg
# if not specified, default par_viash to look for 'viash' on the PATH
if [ -z "$par_viash" ]; then
  par_viash="viash"
fi

# if specified, use par_max_threads as a java argument
if [ ! -z "$par_max_threads" ]; then
  export JAVA_ARGS="$JAVA_ARGS -Dscala.concurrent.context.maxThreads=$par_max_threads"
fi

# process queries
if [ ! -z "$par_query" ]; then
  command_builder+=("--query" "$par_query")
fi
if [ ! -z "$par_query_namespace" ]; then
  command_builder+=("--query_name" "$par_query_name")
fi
if [ ! -z "$par_query_name" ]; then
  command_builder+=("--query_namespace" "$par_query_namespace")
fi

# process config mods
if [ ! -z "$par_config_mod" ]; then
  IFS=";"
  for var in $par_config_mod; do
    unset IFS
    command_builder+=("--config_mod" "$var")
  done
fi

if [ ! -z "$par_registry" ]; then
  command_builder+=(
    --config_mod ".platforms[.type == 'docker'].target_registry := '$par_registry'"
    --config_mod ".platforms[.type == 'nextflow'].registry := '$par_registry'"
  )
fi

if [ ! -z "$par_organization" ]; then
  command_builder+=(
    --config_mod ".platforms[.type == 'docker'].target_organization := '$par_organization'"
    --config_mod ".platforms[.type == 'nextflow'].organization := '$par_organization'"
  )
fi

if [ ! -z "$par_namespace_separator" ]; then
  command_builder+=(
    --config_mod ".platforms[.type == 'docker' || .type == 'nextflow'].namespace_separator := '$par_namespace_separator'"
  )
fi

if [ ! -z "$par_target_image_source" ]; then
  command_builder+=(
    --config_mod ".platforms[.type == 'docker'].target_image_source := '$par_target_image_source'"
  )
fi

if [ ! -z "$par_platform" ]; then
  command_builder+=(--platform "$par_platform")
fi

################ RUN COMMAND ################
[[ "$par_verbose" == "true" ]] && echo "+ $par_viash" "${command_builder[@]}"

if [ -z "$par_log" ]; then
  "$par_viash" "${command_builder[@]}"
else
  rm "$par_log"
  "$par_viash" "${command_builder[@]}" > >(tee -a "$par_log") 2> >(tee -a "$par_log")
fi
