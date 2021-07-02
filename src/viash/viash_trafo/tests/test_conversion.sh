#!/usr/bin/env bash
set -ex

echo ">>> Checking whether output is correct"

# trafo in one direction
./vshtrafo -i config.vsh.yaml -o config2script -f script > /dev/null

./vshtrafo -i config2script/script.vsh.sh -o config2script2config -f config > /dev/null

# compare
yq x -P config.vsh.yaml config2script2config/config.vsh.yaml
diff script.sh config2script2config/script.sh

# trafo in other direction
./vshtrafo -i script.vsh.sh -o script2config -f config > /dev/null

./vshtrafo -i script2config/config.vsh.yaml -o script2config2script -f script > /dev/null

# compare
diff script.vsh.sh script2config2script/script.vsh.sh

echo ">>> Done!"
