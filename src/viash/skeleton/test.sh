#!/bin/bash

###########################################
###       TESTING PYTHON SKELETON       ###
###########################################
./skeleton -n testpy -ns test -l py

[[ ! -f src/test/testpy/config.vsh.yaml ]] && echo "Output file could not be found!" && exit 1

viash test src/test/testpy/config.vsh.yaml

[ $? -ne 0 ] && echo "Python component finished unsuccessfully" && exit 1


###########################################
###         TESTING R SKELETON          ###
###########################################

./skeleton -n testr -ns test -l r

[[ ! -f src/test/testr/config.vsh.yaml ]] && echo "Output file could not be found!" && exit 1

viash test src/test/testr/config.vsh.yaml

[ $? -ne 0 ] && echo "R component finished unsuccessfully" && exit 1



###########################################
###        TESTING BASH SKELETON        ###
###########################################

./skeleton -n testbash -ns test -l bash

[[ ! -f src/test/testbash/config.vsh.yaml ]] && echo "Output file could not be found!" && exit 1

viash test src/test/testbash/config.vsh.yaml

[ $? -ne 0 ] && echo "Bash component finished unsuccessfully" && exit 1

###########################################
###            WRAP UP TESTS            ###
###########################################
# print final message
echo ">>> Test finished successfully"

# do not remove this
# as otherwise your test might exit with a different exit code
exit 0
