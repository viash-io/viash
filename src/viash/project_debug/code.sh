#!/bin/bash

# set -ex

function output {
  echo "$@" >> $par_output
}

echo "# Debug Report `date`" > $par_output
output ""

output "## Overview"
output ""

# Retrieve information about errors
cat $par_input | grep ERROR > /dev/null
contains_errors=$?
if [ $contains_errors -eq 0 ]; then
  errors=1
  cat $par_input | grep ERROR > /$par_tmp/failed.tsv
else
  errors=0
fi

# Retrieve information about missings
cat $par_input | grep MISSING > /dev/null
contains_missings=$?
if [ $contains_missings -eq 0 ]; then
  missings=1
  cat $par_input | grep MISSING > $par_tmp/missing.tsv
else
  missings=0
fi

output "Failed components:"
output ""
if [ $errors -eq 1 ]; then
  cat /$par_tmp/failed.tsv | while read f; do
    ns=`echo -n "$f" | cut -f1`
    comp=`echo -n "$f" | cut -f2`
    platform=`echo -n "$f" | cut -f3`
    output "- \`$comp\` in \`$ns\`, platform \`$platform\`"
  done
  output ""
else
  output "No failed components"
  output ""
fi

output "Missing components:"
output ""
if [ $missings -eq 1 ]; then
  cat $par_tmp/missing.tsv | while read f; do
    ns=`echo -n "$f" | cut -f1`
    comp=`echo -n "$f" | cut -f2`
    platform=`echo -n "$f" | cut -f3`
    output "- \`$comp\` in \`$ns\`, platform \`$platform\`"
  done
  output ""
else
  output "No missing components"
  output ""
fi

if [ $errors -eq 1 ]; then

  output ""
  output "## Error report"
  output ""

  cat /$par_tmp/failed.tsv | while read f; do
    ns=`echo -n "$f" | cut -f1`
    comp=`echo -n "$f" | cut -f2`
    platform=`echo -n "$f" | cut -f3`
    root_test_dir=`ls -ctd "$par_tmp/viash_test_$comp"* | head -1`
    output "### \`$comp\` Build"
    output ""
    output "Files:"
    output ""
    output '```'
    ls -alh "$root_test_dir/build_executable" > $par_tmp/list.log
    cat $par_tmp/list.log >> $par_output
    output '```'
    output ""
    output "Build log:"
    output '```'
    cat "$root_test_dir/build_executable/_viash_build_log.txt" >> $par_output
    output '```'

    output "### \`$comp\` Test"
    output ""
    output "Files:"
    output ""
    output '```'
    ls -alh "$root_test_dir/test_run"* > $par_tmp/list.log
    cat $par_tmp/list.log >> $par_output
    output '```'
    output ""
    output "Setup log:"
    output '```'
    cat "$root_test_dir/test_"*"/_viash_test_log.txt" >> $par_output
    output '```'
  done
  output ""

fi
