#!/bin/bash

# set -ex

function output {
  echo "$@" >> $par_output
}

echo "# Debug Report `date`" > $par_output
output ""
output "This reports uses the provided tsv log file to retrieve components"
output "that gave errors during a \`viash ns test\` test run."

output ""
output "In _append_ mode, additional test results are added to the tsv log file,"
output "so an error may already be resolved but still represented here."

output ""
output "In general, the following situations are possible:"
output ""
output "1. A component gives no errors, all builds and tests runs well for every platform"
output "2. A component fails for a given platform, either during build or test"
output "  a. There is at least one failure in the tsv log file, but the last entry is a success."
output "  b. The last run for this component failed."
output ""

# Retrieve information about errors
cat $par_input | grep ERROR > /dev/null
contains_errors=$?
if [ $contains_errors -eq 0 ]; then
  errors=1
  cat $par_input | grep ERROR > $par_tmp/failed.tsv
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

# Retrieve information about success
cat $par_input | grep SUCCES > /dev/null
contains_success=$?
if [ $contains_success -eq 0 ]; then
  success=1
  cat $par_input | grep SUCCESS > $par_tmp/success.tsv
else
  success=0
fi
# Start writing content
output "## Overview"
output ""

output "Failed components:"
output ""
if [ $errors -eq 1 ]; then
  cat $par_tmp/failed.tsv | cut -f1,2,3 | sort | uniq | while read f; do
    ns=`echo -n "$f" | cut -f1`
    comp=`echo -n "$f" | cut -f2`
    platform=`echo -n "$f" | cut -f3`
    still_exec=`cat $par_input | grep -P "$ns\t$comp\t$platform" | tail -1 | grep ERROR`
    still=$?
    if [ $still -eq 0 ]; then
      line="- \`$comp\` in \`$ns\`, platform \`$platform\` and is still open. See full report below."
    else
      line="- \`$comp\` in \`$ns\`, platform \`$platform\` but is resolved."
    fi
    output "$line"
  done
  output ""
else
  output "No failed components"
  output ""
fi

output "Missing components:"
output ""
if [ $missings -eq 1 ]; then
  cat $par_tmp/missing.tsv | cut -f1,2,3 | sort | uniq | while read f; do
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

# output "Working components:"
# output ""
# if [ $success -eq 1 ]; then
#   cat $par_tmp/success.tsv | cut -f1,2,3 | sort | uniq | while read f; do
#     ns=`echo -n "$f" | cut -f1`
#     comp=`echo -n "$f" | cut -f2`
#     platform=`echo -n "$f" | cut -f3`
#     output "- \`$comp\` in \`$ns\`, platform \`$platform\`"
#   done
#   output ""
# else
#   output "No successfull components"
#   output ""
# fi

if [ $errors -eq 1 ]; then

  output ""
  output "## Error report"
  output ""

  cat $par_tmp/failed.tsv | cut -f1,2,3 | sort | uniq | while read f; do
    ns=`echo -n "$f" | cut -f1`
    comp=`echo -n "$f" | cut -f2`
    platform=`echo -n "$f" | cut -f3`
    still_exec=`cat $par_input | grep -P "$ns\t$comp\t$platform" | tail -1 | grep ERROR`
    still=$?
    root_test_dir=`ls -ctd "$par_tmp/viash_test_$comp"* | head -1`

    if [ $still -eq 0 ]; then

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
      output ""
      output '```'
      cat "$root_test_dir/build_executable/_viash_build_log.txt" >> $par_output
      output '```'
      output ""

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
      output ""
      output '```'
      cat "$root_test_dir/test_"*"/_viash_test_log.txt" >> $par_output
      output '```'
      output ""
    fi

  done

fi
