#!/usr/bin/env bash
set -ex

echo ">>> Checking whether output is correct"

# trafo in one direction
./vshtrafo -i functionality.yaml -o split_joined -f joined > /dev/null

./vshtrafo -i split_joined/config.vsh.yaml -o joined_script -f script > /dev/null

./vshtrafo -i joined_script/script.vsh.sh -o script_split -f split > /dev/null

# compare
yq x -P functionality.yaml script_split/functionality.yaml
yq x -P platform_native.yaml script_split/platform_native.yaml
yq x -P platform_docker.yaml script_split/platform_docker.yaml
diff script.sh script_split/script.sh

# trafo in other direction
./vshtrafo -i functionality.yaml -o split_script -f script > /dev/null

./vshtrafo -i split_script/script.vsh.sh -o script_joined -f joined > /dev/null

./vshtrafo -i script_joined/config.vsh.yaml -o joined_split -f split > /dev/null

# compare
yq x -P functionality.yaml joined_split/functionality.yaml
yq x -P platform_native.yaml joined_split/platform_native.yaml
yq x -P platform_docker.yaml joined_split/platform_docker.yaml
diff script.sh joined_split/script.sh

echo ">>> Done!"
