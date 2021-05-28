#!/bin/bash

if [ "$par_mode" == "release" ]; then
  echo "In release mode..."
  if [ "$par_tag" == "dev" ]; then
    echo "For a release, you have to specify an explicit version using --version"
    exit 1
  else
    echo "Using version $par_tag" to tag containers
  fi
fi

# if not specified, default queries to a catch-all regexes
if [ -z "$par_query" ]; then
  par_query=".*"
fi
if [ -z "$par_query_namespace" ]; then
  par_query_namespace=".*"
fi
if [ -z "$par_query_name" ]; then
  par_query_name=".*"
fi

# if not specified, default par_viash to look for 'viash' on the PATH
if [ -z "$par_viash" ]; then
  par_viash="viash"
fi


# if specified, use par_max_threads as a java argument
if [ ! -z "$par_max_threads" ]; then
  export JAVA_ARGS="$JAVA_ARGS -Dscala.concurrent.context.maxThreads=$par_max_threads"
fi

if [[ $par_force == true ]]; then
  echo "Force push... handle with care..."
  if [ "$par_mode" == "development" ]; then
    echo "No container push can and should be performed in this mode"
  elif [ "$par_mode" == "integration" ]; then
    "$par_viash" ns build \
      --platform "docker" \
      --query "$par_query" \
      --query_name "$par_query_name" \
      --query_namespace "$par_query_namespace" \
      -c '.functionality.version := "dev"' \
      -c '.platforms[.type == "docker"].target_registry := "'"$par_registry"'"' \
      -c '.platforms[.type == "docker"].setup_strategy := "donothing"' \
      -c '.platforms[.type == "docker"].push_strategy := "alwayspush"' \
      -c '.platforms[.type == "nextflow"].registry := "'"$par_registry"'"' \
      -c '.platforms[.type == "docker" || .type == "nextflow"].namespace_separator := "'$par_namespace_separator'"' \
      -c "$par_config_mod" \
      -l \
      --setup --push | tee "$par_log"
  elif [ "$par_mode" == "release" ]; then
    "$par_viash" ns build \
      --platform "docker" \
      --query "$par_query" \
      --query_name "$par_query_name" \
      --query_namespace "$par_query_namespace" \
      -c '.functionality.version := "'"$par_tag"'"' \
      -c '.platforms[.type == "docker"].target_registry := "'"$par_registry"'"' \
      -c '.platforms[.type == "docker"].setup_strategy := "donothing"' \
      -c '.platforms[.type == "docker"].push_strategy := "alwayspush"' \
      -c '.platforms[.type == "nextflow"].registry := "'"$par_registry"'"' \
      -c '.platforms[.type == "docker" || .type == "nextflow"].namespace_separator := "'$par_namespace_separator'"' \
      -c "$par_config_mod" \
      -l \
      --setup --push | tee "$par_log"
  else
    echo "Not a valid mode argument"
  fi
else
  if [ "$par_mode" == "development" ]; then
    echo "No container push can and should be performed in this mode"
  elif [ "$par_mode" == "integration" ]; then
    "$par_viash" ns build \
      --platform "docker" \
      --query "$par_query" \
      --query_name "$par_query_name" \
      --query_namespace "$par_query_namespace" \
      -c '.functionality.version := "dev"' \
      -c '.platforms[.type == "docker"].target_registry := "'"$par_registry"'"' \
      -c '.platforms[.type == "docker"].setup_strategy := "donothing"' \
      -c '.platforms[.type == "nextflow"].registry := "'"$par_registry"'"' \
      -c '.platforms[.type == "docker" || .type == "nextflow"].namespace_separator := "'$par_namespace_separator'"' \
      -c "$par_config_mod" \
      -l \
      --setup --push | tee "$par_log"
  elif [ "$par_mode" == "release" ]; then
    "$par_viash" ns build \
      --platform "docker" \
      --query "$par_query" \
      --query_name "$par_query_name" \
      --query_namespace "$par_query_namespace" \
      -c '.functionality.version := "'"$par_tag"'"' \
      -c '.platforms[.type == "docker"].target_registry := "'"$par_registry"'"' \
      -c '.platforms[.type == "docker"].setup_strategy := "donothing"' \
      -c '.platforms[.type == "nextflow"].registry := "'"$par_registry"'"' \
      -c '.platforms[.type == "docker" || .type == "nextflow"].namespace_separator := "'$par_namespace_separator'"' \
      -c "$par_config_mod" \
      -l \
      --setup --push | tee "$par_log"
  else
    echo "Not a valid mode argument"
  fi
fi
