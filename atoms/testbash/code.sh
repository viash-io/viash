#!/bin/bash

# PORTASH START
declare -A PAR
PAR["abc"]=1
PAR["in"]="testbash"
# PORTASH END

echo "## This is your abc parameter: ${PAR["abc"]}"
echo "## Now reading from file '${PAR["in"]}'"

cat "${PAR["in"]}"
