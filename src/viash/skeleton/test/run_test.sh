#!/usr/bin/env bash
set -ex

echo ">>> Checking whether output is generated"
./skeleton \
  --name test_skeleton \
  --namespace test_namespace \
  --src my_src

[[ ! -d my_src ]] && echo "It seems no src dir is generated" && exit 1
[[ ! -d my_src/test_namespace ]] && echo "The namespace dir was not generated" && exit 1
[[ ! -d my_src/test_namespace/test_skeleton ]] && echo "The component dir was not generated" && exit 1
[[ ! -f my_src/test_namespace/test_skeleton/script.sh  ]] && echo "The skeleton script.sh was not written" && exit 1
[[ ! -f my_src/test_namespace/test_skeleton/config.vsh.yaml  ]] && echo "The skeleton config.vsh.yaml was not written" && exit 1

echo ">>> Test finished successfully"
