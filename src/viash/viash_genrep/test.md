# Debug Report Fri Mar 26 14:16:07 UTC 2021

## Overview

Failed components:

- `skeleton` in `viash`, platform `native`

Missing components:

- `project_debug` in `viash`, platform `native`
- `project_debug` in `viash`, platform `docker`
- `project_doc` in `viash`, platform `native`
- `project_doc` in `viash`, platform `docker`


## Error report

### `skeleton` Build

Files:

```
total 20K
drwxr-xr-x 6 root root  192 Mar 26 09:16 .
drwx------ 4 root root  128 Mar 26 09:16 ..
-rw-r--r-- 1 root root  148 Mar 26 09:16 _viash_build_log.txt
-rwxr--r-- 1 root root 4.9K Mar 26 09:16 skeleton
-rw-r--r-- 1 root root  174 Mar 26 09:16 skeleton.sh
-rw-r--r-- 1 root root  702 Mar 26 09:16 skeleton.yaml
```

Build log:
```
====================================================================
+/tmp/viash_test_skeleton775554152066754314/build_executable/skeleton ---setup
```
### `skeleton` Test

Files:

```
total 28K
drwxr-xr-x 8 root root  256 Mar 26 09:16 .
drwx------ 4 root root  128 Mar 26 09:16 ..
-rw-r--r-- 1 root root  641 Mar 26 09:16 _viash_test_log.txt
drwxr-xr-x 3 root root   96 Mar 26 09:16 my_src
-rwxr--r-- 1 root root 4.1K Mar 26 09:16 run_test.sh
-rwxr--r-- 1 root root 4.9K Mar 26 09:16 skeleton
-rw-r--r-- 1 root root  174 Mar 26 09:16 skeleton.sh
-rw-r--r-- 1 root root  702 Mar 26 09:16 skeleton.yaml
```

Setup log:
```
====================================================================
+/tmp/viash_test_skeleton775554152066754314/test_run_test.sh/run_test.sh
>>> Checking whether output is generated
+ echo '>>> Checking whether output is generated'
+ ./skeleton --name test_skeleton --namespace test_namespace --src my_src
+ [[ ! -d my_src ]]
+ [[ ! -d my_src/test_namespace ]]
+ [[ ! -d my_src/test_namespace/test_skeleton ]]
+ [[ ! -f my_src/test_namespace/test_skeleton/script.sh ]]
+ [[ -f my_src/test_namespace/test_skeleton/config.vsh.yaml ]]
+ echo 'The skeleton config.vsh.yaml was not written'
The skeleton config.vsh.yaml was not written
+ exit 1
```

