#!/usr/bin/env bash
set -ex

echo ">>> Checking whether output is correct"

./vshtrafo -i functionality.yaml -o split_joined -f joined > /dev/null
diff script.sh split_joined/script.sh
yq compare joined.vsh.yaml split_joined/config.vsh.yaml -P

# wip
./vshtrafo -i functionality.yaml -o split_script -f script > /dev/null

./vshtrafo -i joined.vsh.yaml -o joined_split -f split > /dev/null

./vshtrafo -i joined.vsh.yaml -o joined_script -f script > /dev/null

./vshtrafo -i script.vsh.sh -o script_split -f split > /dev/null

./vshtrafo -i script.vsh.sh -o script_joined -f joined > /dev/null
