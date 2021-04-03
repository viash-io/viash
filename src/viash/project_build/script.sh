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

if [ "$par_mode" == "development" ]; then
  echo "In development mode..."
  
  if [ "$par_no_cache" == "true" ]; then
    setup_strat="build"
  else 
    setup_strat="cachedbuild"
  fi
  
  "$par_viash" ns build \
    --platform "$par_platforms" \
    --query "$par_query" \
    --query_name "$par_query_name" \
    --query_namespace "$par_query_namespace" \
    -c '.functionality.version := "dev"' \
    -c '.platforms[.type == "docker"].setup_strategy := "'$setup_strat'"' \
    -l -w \
    --setup | tee "$par_log"
elif [ "$par_mode" == "integration" ]; then
  echo "In integration mode..."
  
  if [ "$par_no_cache" == "true" ]; then
    echo "Warning: '--no_cache' only applies when '--mode=development'."
  fi
  
  "$par_viash" ns build \
    --platform "$par_platforms" \
    --query "$par_query" \
    --query_name "$par_query_name" \
    --query_namespace "$par_query_namespace" \
    -c '.functionality.version := "'"$par_version"'"' \
    -c '.platforms[.type == "docker"].target_registry := "'"$par_registry"'"' \
    -c '.platforms[.type == "docker"].setup_strategy := "build"' \
    -c '.platforms[.type == "nextflow"].registry := "'"$par_registry"'"' \
    -l -w \
    --setup | tee "$par_log"
elif [ "$par_mode" == "release" ]; then
  echo "In release mode..."
  
  if [ "$par_no_cache" == "true" ]; then
    echo "Warning: '--no_cache' only applies when '--mode=development'."
  fi
  
  if [ "$par_version" == "dev" ]; then
    echo "Error: For a release, you have to specify an explicit version using --version"
    exit 1
  fi
  "$par_viash" ns build \
    --platform "$par_platforms" \
    --query "$par_query" \
    --query_name "$par_query_name" \
    --query_namespace "$par_query_namespace" \
    -c '.functionality.version := "'"$par_version"'"' \
    -c '.platforms[.type == "docker"].target_registry := "'"$par_registry"'"' \
    -c '.platforms[.type == "docker"].setup_strategy := "build"' \
    -c '.platforms[.type == "nextflow"].registry := "'"$par_registry"'"' \
    -l -w \
    --setup | tee "$par_log"
else
  echo "Not a valid mode argument"
fi
