#!/bin/bash

viash build config.vsh.yaml -p nextflow -o test/

nextflow run test/main.nf --id myid --output out/ --input1 input1.txt --input2 input2.txt
[ ! -f out/myid/myid.tcomp_multiinput.output ] && echo "Output file was not written" && exit 1

# print final message
echo ">>> Test finished successfully"

rm -r test
rm -rf work
rm -r out
rm -rf .nextflow/
rm .nextflow.*
