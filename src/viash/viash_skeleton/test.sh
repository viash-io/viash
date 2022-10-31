#!/bin/bash

set -ex


###########################################
###        TESTING BASH SKELETON        ###
###########################################

$meta_executable -n testbash -ns test -l bash --src my_src


echo ">>> Checking whether output is generated"

[[ ! -d my_src ]] && echo "It seems no src dir is generated" && exit 1
[[ ! -d my_src/test ]] && echo "The namespace dir was not generated" && exit 1
[[ ! -d my_src/test/testbash ]] && echo "The component dir was not generated" && exit 1
[[ ! -f my_src/test/testbash/script.sh  ]] && echo "The skeleton script.sh was not written" && exit 1
[[ ! -f my_src/test/testbash/config.vsh.yaml  ]] && echo "The skeleton config.vsh.yaml was not written" && exit 1

viash test my_src/test/testbash/config.vsh.yaml

[ $? -ne 0 ] && echo "Bash component finished unsuccessfully" && exit 1


###########################################
###       TESTING PYTHON SKELETON       ###
###########################################
$meta_executable -n testpy -ns test -l py

[[ ! -f src/test/testpy/config.vsh.yaml ]] && echo "Output file could not be found!" && exit 1

viash test src/test/testpy/config.vsh.yaml

[ $? -ne 0 ] && echo "Python component finished unsuccessfully" && exit 1


###########################################
###         TESTING R SKELETON          ###
###########################################

$meta_executable -n testr -ns test -l r

[[ ! -f src/test/testr/config.vsh.yaml ]] && echo "Output file could not be found!" && exit 1

viash test src/test/testr/config.vsh.yaml

[ $? -ne 0 ] && echo "R component finished unsuccessfully" && exit 1








###########################################
###            WRAP UP TESTS            ###
###########################################
# print final message
echo ">>> Test finished successfully"

# do not remove this
# as otherwise your test might exit with a different exit code
exit 0
