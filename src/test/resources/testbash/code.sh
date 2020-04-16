#!/bin/bash

# PORTASH START
declare -A par
par_input="input.sh"
par_real_number="123.987654"
par_whole_number="17"
par_s="test string"
par_truth="true"
par_output="output.txt"
par_log="log.txt"
par_optional="help"
par_optional_with_default="me"
# PORTASH END

function log {
  if [ -z "$par_log" ]; then
    echo $@
  else
    echo $@ > $par_log
  fi
}

log "INFO: Parsed input arguments."

if [ -z "$par_output" ]; then
  log "INFO: Printing output to console"
  typeset -p | grep par_ | sed 's#"$##' | sed 's#.*par_\([^=]*\)="*\(.*\)$#\1: "\2"#'
else
  log "INFO: Writing output to file"
  typeset -p | grep par_ | sed 's#"$##' | sed 's#.*par_\([^=]*\)="*\(.*\)$#"\1": "\2",#' | tr '\n' ' ' | sed 's#\(.*\), #{\1}\n#' > $par_output
fi


