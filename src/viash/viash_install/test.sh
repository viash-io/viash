#!/bin/bash

# set -ex

###############################################################################
echo ">> Test default options, should install latest in current dir"
$meta_executable 
e=$?

echo ">>> Checking whether output is generated"
[ $e -ne 0 ] && echo "Viash did not exit successfully" && exit 1
[[ ! -f viash ]] && echo "It seems the Viash executable is not created" && exit 1

###############################################################################
echo ">> Test alternative (release) version"
$meta_executable --tag 0.6.6
e=$?

echo ">>> Checking whether output is generated"
version=`./viash -v | sed 's#viash \([^ ]*\) .*#\1#'`
[ $e -ne 0 ] && echo "Viash did not exit successfully" && exit 1
[[ ! -f viash ]] && echo "It seems the Viash executable is not created" && exit 1
[[ ! "$version" == '0.6.6' ]] && echo "It seems the version does not match" && exit 1

###############################################################################
echo "> Test alternative (develop) version"
$meta_executable --tag develop
e=$?

echo ">>> Checking whether output is generated"
[ $e -ne 0 ] && echo "Viash did not exit successfully" && exit 1
[[ ! -f viash ]] && echo "It seems the Viash executable is not created" && exit 1

###############################################################################
echo ">> Test alternative location"
$meta_executable --output ./my_dir/my_viash
e=$?

echo ">>> Checking whether output is generated"
[ $e -ne 0 ] && echo "Viash did not exit successfully" && exit 1
[[ ! -f "./my_dir/my_viash" ]] && echo "It seems the Viash executable is not created" && exit 1

###############################################################################
# print final message
echo ">> Test finished successfully"

# do not remove this
# as otherwise your test might exit with a different exit code
exit 0

