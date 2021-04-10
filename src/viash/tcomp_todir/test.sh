#!/bin/bash

viash build config.vsh.yaml -p nextflow -o test/

nextflow run test/main.nf --id myid --output out/ --input input.txt
[ ! -d out/myid/myid.tcomp_todir.output ] && echo "Output directory was not written" && exit 1

# print final message
echo ">>> Test finished successfully"

rm -r test
rm -rf work
rm -r out
rm -rf .nextflow/
rm .nextflow.*
