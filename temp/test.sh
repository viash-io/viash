#!/bin/bash

echo "Fetch the test.txt file from in/ and store the filtered version in out/"

echo "...run"
output/wrapped_filter_docker/wrapped_filter \
  --in "$PWD/in" \
  --out "$PWD/out" \
  --input "/in/test.txt" \
  --output "/out/output.txt" \
  --filter "echo" \
  -b

echo "test..."
result=`grep "echo" out/output.txt`
if [[ $? == 0 ]]; then
  echo "OK"
else
  echo "NOK"
fi

