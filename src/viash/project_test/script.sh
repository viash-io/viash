#!/bin/bash

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

# if --append (-a) true is specified, add `--append`
if [ "$par_append" == "true" ]; then
  par_append_parsed="--append"
fi


# if specified, use par_max_threads as a java argument
if [ ! -z "$par_max_threads" ]; then
  export JAVA_ARGS="$JAVA_ARGS -Dscala.concurrent.context.maxThreads=$par_max_threads"
fi

if [ "$par_mode" == "development" ]; then
  echo "In development mode..."
  "$par_viash" ns test \
    --platform "$par_platforms" \
    --query "$par_query" \
    --query_name "$par_query_name" \
    --query_namespace "$par_query_namespace" \
    -c '.functionality.version := "dev"' \
    -c '.platforms[.type == "docker"].setup_strategy := "donothing"' \
    -c '.platforms[.type == "docker" || .type == "nextflow"].namespace_separator := "'$par_namespace_separator'"' \
    -c "$par_config_mod" \
    -l \
    -t "$par_log" \
    $par_append_parsed
elif [ "$par_mode" == "integration" ]; then
  echo "In integration mode..."
  "$par_viash" ns test \
    --platform "$par_platforms" \
    --query "$par_query" \
    --query_name "$par_query_name" \
    --query_namespace "$par_query_namespace" \
    -c '.functionality.version := "'"$par_version"'"' \
    -c '.platforms[.type == "docker"].target_registry := "'"$par_registry"'"' \
    -c '.platforms[.type == "docker"].setup_strategy := "donothing"' \
    -c '.platforms[.type == "nextflow"].registry := "'"$par_registry"'"' \
    -c '.platforms[.type == "docker" || .type == "nextflow"].namespace_separator := "'$par_namespace_separator'"' \
    -c "$par_config_mod" \
    -l \
    -t "$par_log" \
    $par_append_parsed
elif [ "$par_mode" == "release" ]; then
  echo "In release mode..."
  if [ "$par_version" == "dev" ]; then
    echo "For a release, you have to specify an explicit version using --version"
    exit 1
  fi
  "$par_viash" ns test \
    --platform "$par_platforms" \
    --query "$par_query" \
    --query_name "$par_query_name" \
    --query_namespace "$par_query_namespace" \
    -c '.functionality.version := "'"$par_version"'"' \
    -c '.platforms[.type == "docker"].target_registry := "'"$par_registry"'"' \
    -c '.platforms[.type == "docker"].setup_strategy := "pull"' \
    -c '.platforms[.type == "nextflow"].registry := "'"$par_registry"'"' \
    -c '.platforms[.type == "docker" || .type == "nextflow"].namespace_separator := "'$par_namespace_separator'"' \
    -c "$par_config_mod" \
    -l \
    -t "$par_log" \
    $par_append_parsed
else
  echo "Not a valid mode argument"
fi
