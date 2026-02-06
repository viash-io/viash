# debug bash component

The bash test_languages component (`src/test/resources/test_languages/bash/config.vsh.yaml`) is failing when being run using a docker backend. Interestingly, the other components (r, python, js, ...) all work fine with docker, and these components are using the exact same unit test (since the interface is the same.) In a native runner, it also works.

## Build prior to testing

```bash
make touch && make
```

## Test component with native engine

This works:

```bash
VIASH_TEMP=debug/test_native bin/viash test src/test/resources/test_languages/bash/config.vsh.yaml --engine native --keep true
```

    Running tests in temporary directory: '/tmp/viash_test_test_languages_bash_3349388423719176516'
    ====================================================================
    +/tmp/viash_test_test_languages_bash_3349388423719176516/test_test/test_executable
    >>> Checking whether expected resources exist
    >>> Checking whether output is correct
    >>> Checking whether output is correct with minimal parameters
    >>> Checking whether output is correct with minimal parameters, but with 1024-base memory
    >>> Test finished successfully
    ====================================================================
    SUCCESS! All 1 out of 1 test scripts succeeded!
    Cleaning up temporary directory

```bash
tree debug/test_native
```

    debug/test_native
    └── viash_test_test_languages_bash_3464054071439338915
        └── test_test
            ├── log.txt
            ├── output2.txt
            ├── output.txt
            ├── resource1.txt
            ├── resource2.txt
            ├── test_executable
            ├── test_languages_bash
            ├── tmp
            │   └── temp
            │       ├── viash-run-test_languages_bash-workdir-BHhZeL
            │       │   ├── params.json
            │       │   └── script.sh
            │       ├── viash-run-test_languages_bash-workdir-yy5hB0
            │       │   ├── params.json
            │       │   └── script.sh
            │       └── viash-run-test_languages_bash-workdir-z0OfoC
            │           ├── params.json
            │           └── script.sh
            └── _viash_test_log.txt

    8 directories, 14 files

## Test component with docker engine

This doesn't:

```bash
VIASH_TEMP=debug/test bin/viash test src/test/resources/test_languages/bash/config.vsh.yaml --engine docker --keep true
```

    Running tests in temporary directory: '/home/rcannood/workspace/viash-io/viash/debug/test/viash_test_test_languages_bash_7237834006593708911'
    ====================================================================
    +/home/rcannood/workspace/viash-io/viash/debug/test/viash_test_test_languages_bash_7237834006593708911/build_engine_environment/test_languages_bash ---verbosity 6 ---setup cachedbuild ---engine docker
    [notice] Building container 'test_languages_bash:test' with Dockerfile
    [info] docker build -t 'test_languages_bash:test'  '/home/rcannood/workspace/viash-io/viash/debug/test/viash_test_test_languages_bash_7237834006593708911/build_engine_environment' -f '/home/rcannood/workspace/viash-io/viash/debug/test/viash_test_test_languages_bash_7237834006593708911/build_engine_environment/tmp/dockerbuild-test_languages_bash-lAvFzX/Dockerfile'
    #0 building with "default" instance using docker driver

    #1 [internal] load build definition from Dockerfile
    #1 DONE 0.0s

    #1 [internal] load build definition from Dockerfile
    #1 transferring dockerfile: 508B done
    #1 DONE 0.1s

    #2 [internal] load metadata for docker.io/library/bash:3.2
    #2 DONE 0.0s

    #3 [internal] load .dockerignore
    #3 transferring context: 2B done
    #3 DONE 0.1s

    #4 [1/1] FROM docker.io/library/bash:3.2
    #4 CACHED

    #5 exporting to image
    #5 exporting layers done
    #5 writing image sha256:1e8ac5c2fb7f52d1da08b50c025d36b1285fe3495595018f7560a2d0952ae19e done
    #5 naming to docker.io/library/test_languages_bash:test 0.0s done
    #5 DONE 0.1s
    ====================================================================
    +/home/rcannood/workspace/viash-io/viash/debug/test/viash_test_test_languages_bash_7237834006593708911/test_test/test_executable
    >>> Checking whether expected resources exist
    >>> Checking whether output is correct
    [notice] Keeping work directory at '/viash_automount/home/rcannood/workspace/viash-io/viash/debug/test/viash_test_test_languages_bash_7237834006593708911/test_test/tmp/temp/viash-run-test_languages_bash-workdir-KiEmAI' because VIASH_KEEP_WORK_DIR is set.
    >>> Checking whether output is correct with minimal parameters
    /viash_automount/home/rcannood/workspace/viash-io/viash/debug/test/viash_test_test_languages_bash_7237834006593708911/test_test/tmp/temp/viash-run-test_languages_bash-workdir-LFfOJN/script.sh: line 217: f: command not found
    [notice] Keeping work directory at '/viash_automount/home/rcannood/workspace/viash-io/viash/debug/test/viash_test_test_languages_bash_7237834006593708911/test_test/tmp/temp/viash-run-test_languages_bash-workdir-LFfOJN' because VIASH_KEEP_WORK_DIR is set.
    FAILED: Pattern not found: s: |a \\ b \$ c ` d " e ' f \\n g # h @ i { j } k """ l ''' m todo_add_back_DOLLAR_VIASH_TEMP n : o ; p|
    Actual content of relevant lines:
    INFO: Parsed input arguments.
    INFO: Printing output to console
    input: |resource2.txt|
    s: ||
    truth: |false|
    falsehood: |true|
    head of input: |this file is only for testing|
    head of resource1: |if you can read this,|
    multiple_pos: ||
    meta_name: |test_languages_bash|
    meta_resources_dir: |/viash_automount/home/rcannood/workspace/viash-io/viash/debug/test/viash_test_test_languages_bash_7237834006593708911/test_test|
    meta_cpus: |666|
    ---
    ====================================================================
    ERROR! Only 0 out of 1 test scripts succeeded!
    Unexpected error occurred! If you think this is a bug, please post
    create an issue at https://github.com/viash-io/viash/issues containing
    a reproducible example and the stack trace below.

    viash - 0.10.0-dev
    Stacktrace:
    java.lang.RuntimeException: Only 0 out of 1 test scripts succeeded!
            at io.viash.ViashTest$.apply(ViashTest.scala:137)
            at io.viash.Main$.mainCLI(Main.scala:263)
            at io.viash.Main$.mainCLIOrVersioned(Main.scala:131)
            at io.viash.Main$.main(Main.scala:66)
            at io.viash.Main.main(Main.scala)

Note that the relevant output line is missing the expected content:

    s: ||

And that the logs state:

    .../script.sh: line 217: f: command not found

This indicates that something isn't being escaped properly, causing bash to try to interpret the `f` as a command.

## Build and run manually

```bash
bin/viash build src/test/resources/test_languages/bash/config.vsh.yaml --engine docker -o debug/build
```

This works:

```bash
debug/build/test_languages_bash  \
  src/test/resources/test_languages/resource2.txt \
  --real_number 123.456 \
  --whole_number=789 \
  -s "a \\ b \$ c \` d \" e ' f \n g # h @ i { j } k \"\"\" l ''' m todo_add_back_DOLLAR_VIASH_TEMP n : o ; p"
```

    INFO: Parsed input arguments.
    INFO: Printing output to console
    input: |/viash_automount/home/rcannood/workspace/viash-io/viash/src/test/resources/test_languages/resource2.txt|
    real_number: |123.456|
    whole_number: |789|
    long_number: ||
    s: |a \ b $ c ` d " e ' f \n g # h @ i { j } k """ l ''' m todo_add_back_DOLLAR_VIASH_TEMP n : o ; p|
    truth: |false|
    falsehood: |true|
    reality: ||
    output: ||
    log: ||
    optional: ||
    optional_with_default: |The default value.|
    head of input: |this file is only for testing|
    head of resource1: |if you can read this,|
    multiple: ||
    multiple_pos: ||
    meta_name: |test_languages_bash|
    meta_resources_dir: |/viash_automount/home/rcannood/workspace/viash-io/viash/debug/build|
    meta_cpus: |2|
    meta_memory_b: |2000000000|
    meta_memory_kb: |2000000|
    meta_memory_mb: |2000|
    meta_memory_gb: |2|
    meta_memory_tb: |1|
    meta_memory_pb: |1|
    meta_memory_kib: |1953125|
    meta_memory_mib: |1908|
    meta_memory_gib: |2|
    meta_memory_tib: |1|
    meta_memory_pib: |1|

→ Therefore it must be something with how `viash test ...` builds the test (which is backed by docker) and the executable when it is run as inside the test (which is backed by native, inside the test container).

## Troubleshoot test run

Let's troubleshoot the test run:

```bash
$ tree debug/test/
```

    debug/test/
    └── viash_test_test_languages_bash_7237834006593708911
        ├── build_engine_environment
        │   ├── resource1.txt
        │   ├── resource2.txt
        │   ├── test_languages_bash
        │   ├── tmp
        │   └── _viash_build_log.txt
        └── test_test
            ├── log.txt
            ├── output2.txt
            ├── output.txt
            ├── resource1.txt
            ├── resource2.txt
            ├── test_executable
            ├── test_languages_bash
            ├── tmp
            │   └── temp
            │       ├── viash-run-test_languages_bash-workdir-KiEmAI
            │       │   ├── params.json
            │       │   └── script.sh
            │       └── viash-run-test_languages_bash-workdir-LFfOJN
            │           ├── params.json
            │           └── script.sh
            └── _viash_test_log.txt

    9 directories, 16 files

Look at the params json

```bash
cat debug/test/viash_test_test_languages_bash_7237834006593708911/test_test/tmp/temp/viash-run-test_languages_bash-workdir-LFfOJN/params.json
```

    {
      "par": {
        "input": "resource2.txt",
        "real_number": 123.456,
        "whole_number": 789,
        "long_number": null,
        "s": "a \\ b $ c ` d \" e ' f \\n g # h @ i { j } k \"\"\" l ''' m todo_add_back_DOLLAR_VIASH_TEMP n : o ; p",
        "truth": false,
        "falsehood": true,
        "reality": null,
        "output": null,
        "log": null,
        "optional": null,
        "optional_with_default": "The default value.",
        "multiple": null,
        "multiple_pos": null
      },
      "meta": {
        "name": "test_languages_bash",
        "resources_dir": "/viash_automount/home/rcannood/workspace/viash-io/viash/debug/test/viash_test_test_languages_bash_7237834006593708911/test_test",
        "executable": "/viash_automount/home/rcannood/workspace/viash-io/viash/debug/test/viash_test_test_languages_bash_7237834006593708911/test_test/test_languages_bash",
        "config": "/viash_automount/home/rcannood/workspace/viash-io/viash/debug/test/viash_test_test_languages_bash_7237834006593708911/test_test/.config.vsh.yaml",
        "temp_dir": "/viash_automount/home/rcannood/workspace/viash-io/viash/debug/test/viash_test_test_languages_bash_7237834006593708911/test_test/tmp/temp",
        "cpus": 666,
        "memory_b": 100000000000000000,
        "memory_kb": 100000000000000,
        "memory_mb": 100000000000,
        "memory_gb": 100000000,
        "memory_tb": 100000,
        "memory_pb": 100,
        "memory_kib": 97656250000000,
        "memory_mib": 95367431641,
        "memory_gib": 93132258,
        "memory_tib": 90950,
        "memory_pib": 89
      },
      "dep": {
      }
    }

Look at the differences between the params.json for the native and docker run:

```bash
diff debug/test/viash_test_test_languages_bash_7237834006593708911/test_test/tmp/temp/viash-run-test_languages_bash-workdir-LFfOJN/params.json debug/test_native/viash_test_test_languages_bash_3464054071439338915/test_test/tmp/temp/viash-run-test_languages_bash-workdir-z0OfoC/params.json
```

    20,23c20,23
    <     "resources_dir": "/viash_automount/home/rcannood/workspace/viash-io/viash/debug/test/viash_test_test_languages_bash_7237834006593708911/test_test",
    <     "executable": "/viash_automount/home/rcannood/workspace/viash-io/viash/debug/test/viash_test_test_languages_bash_7237834006593708911/test_test/test_languages_bash",
    <     "config": "/viash_automount/home/rcannood/workspace/viash-io/viash/debug/test/viash_test_test_languages_bash_7237834006593708911/test_test/.config.vsh.yaml",
    <     "temp_dir": "/viash_automount/home/rcannood/workspace/viash-io/viash/debug/test/viash_test_test_languages_bash_7237834006593708911/test_test/tmp/temp",
    ---
    >     "resources_dir": "/home/rcannood/workspace/viash-io/viash/debug/test_native/viash_test_test_languages_bash_3464054071439338915/test_test",
    >     "executable": "/home/rcannood/workspace/viash-io/viash/debug/test_native/viash_test_test_languages_bash_3464054071439338915/test_test/test_languages_bash",
    >     "config": "/home/rcannood/workspace/viash-io/viash/debug/test_native/viash_test_test_languages_bash_3464054071439338915/test_test/.config.vsh.yaml",
    >     "temp_dir": "/home/rcannood/workspace/viash-io/viash/debug/test_native/viash_test_test_languages_bash_3464054071439338915/test_test/tmp/temp",
    25,35c25,35
    <     "memory_b": 100000000000000000,
    <     "memory_kb": 100000000000000,
    <     "memory_mb": 100000000000,
    <     "memory_gb": 100000000,
    <     "memory_tb": 100000,
    <     "memory_pb": 100,
    <     "memory_kib": 97656250000000,
    <     "memory_mib": 95367431641,
    <     "memory_gib": 93132258,
    <     "memory_tib": 90950,
    <     "memory_pib": 89
    ---
    >     "memory_b": 112589990684262400,
    >     "memory_kb": 112589990684263,
    >     "memory_mb": 112589990685,
    >     "memory_gb": 112589991,
    >     "memory_tb": 112590,
    >     "memory_pb": 113,
    >     "memory_kib": 109951162777600,
    >     "memory_mib": 107374182400,
    >     "memory_gib": 104857600,
    >     "memory_tib": 102400,
    >     "memory_pib": 100

→ There isn't any difference in the how the parameters get stored in the json.

Look at difference between the script.sh for the native and docker run:

```bash
diff debug/test/viash_test_test_languages_bash_7237834006593708911/test_test/tmp/temp/viash-run-test_languages_bash-workdir-LFfOJN/script.sh debug/test_native/viash_test_test_languages_bash_3464054071439338915/test_test/tmp/temp/viash-run-test_languages_bash-workdir-z0OfoC/script.sh
```

→ The script.sh files are identical, so the problem must be in how the script is executed in the test run (which is backed by native, inside the test container) and not in how the script is generated.

```bash
diff debug/test/viash_test_test_languages_bash_7237834006593708911/test_test/test_executable debug/test_native/viash_test_test_languages_bash_3464054071439338915/test_test/test_executable
```

    473,863c473
    < VIASH_ENGINE_ID='docker'
    < 
    < ######## Helper functions for setting up Docker images for viash ########
    < # expects: ViashDockerBuild
    < 
    < # ViashDockerInstallationCheck: check whether Docker is installed correctly
    < #
    < # examples:
    < #   ViashDockerInstallationCheck
    < function ViashDockerInstallationCheck {
    <   ViashDebug "Checking whether Docker is installed"
    <   if [ ! command -v docker &> /dev/null ]; then
    <     ViashCritical "Docker doesn't seem to be installed. See 'https://docs.docker.com/get-docker/' for instructions."
    <     exit 1
    <   fi
    < 
    <   ViashDebug "Checking whether the Docker daemon is running"
    <   local save=$-; set +e
    <   local docker_version=$(docker version --format '{{.Client.APIVersion}}' 2> /dev/null)
    <   local out=$?
    <   [[ $save =~ e ]] && set -e
    <   if [ $out -ne 0 ]; then
    <     ViashCritical "Docker daemon does not seem to be running. Try one of the following:"
    <     ViashCritical "- Try running 'dockerd' in the command line"
    <     ViashCritical "- See https://docs.docker.com/config/daemon/"
    <     exit 1
    <   fi
    < }
    < 
    < # ViashDockerRemoteTagCheck: check whether a Docker image is available 
    < # on a remote. Assumes `docker login` has been performed, if relevant.
    < #
    < # $1                  : image identifier with format `[registry/]image[:tag]`
    < # exit code $?        : whether or not the image was found
    < # examples:
    < #   ViashDockerRemoteTagCheck python:latest
    < #   echo $?                                     # returns '0'
    < #   ViashDockerRemoteTagCheck sdaizudceahifu
    < #   echo $?                                     # returns '1'
    < function ViashDockerRemoteTagCheck {
    <   docker manifest inspect $1 > /dev/null 2> /dev/null
    < }
    < 
    < # ViashDockerLocalTagCheck: check whether a Docker image is available locally
    < #
    < # $1                  : image identifier with format `[registry/]image[:tag]`
    < # exit code $?        : whether or not the image was found
    < # examples:
    < #   docker pull python:latest
    < #   ViashDockerLocalTagCheck python:latest
    < #   echo $?                                     # returns '0'
    < #   ViashDockerLocalTagCheck sdaizudceahifu
    < #   echo $?                                     # returns '1'
    < function ViashDockerLocalTagCheck {
    <   [ -n "$(docker images -q $1)" ]
    < }
    < 
    < # ViashDockerPull: pull a Docker image
    < #
    < # $1                  : image identifier with format `[registry/]image[:tag]`
    < # exit code $?        : whether or not the image was found
    < # examples:
    < #   ViashDockerPull python:latest
    < #   echo $?                                     # returns '0'
    < #   ViashDockerPull sdaizudceahifu
    < #   echo $?                                     # returns '1'
    < function ViashDockerPull {
    <   ViashNotice "Checking if Docker image is available at '$1'"
    <   if [ $VIASH_VERBOSITY -ge $VIASH_LOGCODE_INFO ]; then
    <     docker pull $1 && return 0 || return 1
    <   else
    <     local save=$-; set +e
    <     docker pull $1 2> /dev/null > /dev/null
    <     local out=$?
    <     [[ $save =~ e ]] && set -e
    <     if [ $out -ne 0 ]; then
    <       ViashWarning "Could not pull from '$1'. Docker image doesn't exist or is not accessible."
    <     fi
    <     return $out
    <   fi
    < }
    < 
    < # ViashDockerPush: push a Docker image
    < #
    < # $1                  : image identifier with format `[registry/]image[:tag]`
    < # exit code $?        : whether or not the image was found
    < # examples:
    < #   ViashDockerPush python:latest
    < #   echo $?                                     # returns '0'
    < #   ViashDockerPush sdaizudceahifu
    < #   echo $?                                     # returns '1'
    < function ViashDockerPush {
    <   ViashNotice "Pushing image to '$1'"
    <   local save=$-; set +e
    <   local out
    <   if [ $VIASH_VERBOSITY -ge $VIASH_LOGCODE_INFO ]; then
    <     docker push $1
    <     out=$?
    <   else
    <     docker push $1 2> /dev/null > /dev/null
    <     out=$?
    <   fi
    <   [[ $save =~ e ]] && set -e
    <   if [ $out -eq 0 ]; then
    <     ViashNotice "Container '$1' push succeeded."
    <   else
    <     ViashError "Container '$1' push errored. You might not be logged in or have the necessary permissions."
    <   fi
    <   return $out
    < }
    < 
    < # ViashDockerPullElseBuild: pull a Docker image, else build it
    < #
    < # $1                  : image identifier with format `[registry/]image[:tag]`
    < # ViashDockerBuild    : a Bash function which builds a docker image, takes image identifier as argument.
    < # examples:
    < #   ViashDockerPullElseBuild mynewcomponent
    < function ViashDockerPullElseBuild {
    <   local save=$-; set +e
    <   ViashDockerPull $1
    <   local out=$?
    <   [[ $save =~ e ]] && set -e
    <   if [ $out -ne 0 ]; then
    <     ViashDockerBuild $@
    <   fi
    < }
    < 
    < # ViashDockerSetup: create a Docker image, according to specified docker setup strategy
    < #
    < # $1          : image identifier with format `[registry/]image[:tag]`
    < # $2          : docker setup strategy, see DockerSetupStrategy.scala
    < # examples:
    < #   ViashDockerSetup mynewcomponent alwaysbuild
    < function ViashDockerSetup {
    <   local image_id="$1"
    <   local setup_strategy="$2"
    <   if [ "$setup_strategy" == "alwaysbuild" -o "$setup_strategy" == "build" -o "$setup_strategy" == "b" ]; then
    <     ViashDockerBuild $image_id --no-cache $(ViashDockerBuildArgs "$engine_id")
    <   elif [ "$setup_strategy" == "alwayspull" -o "$setup_strategy" == "pull" -o "$setup_strategy" == "p" ]; then
    <     ViashDockerPull $image_id
    <   elif [ "$setup_strategy" == "alwayspullelsebuild" -o "$setup_strategy" == "pullelsebuild" ]; then
    <     ViashDockerPullElseBuild $image_id --no-cache $(ViashDockerBuildArgs "$engine_id")
    <   elif [ "$setup_strategy" == "alwayspullelsecachedbuild" -o "$setup_strategy" == "pullelsecachedbuild" ]; then
    <     ViashDockerPullElseBuild $image_id $(ViashDockerBuildArgs "$engine_id")
    <   elif [ "$setup_strategy" == "alwayscachedbuild" -o "$setup_strategy" == "cachedbuild" -o "$setup_strategy" == "cb" ]; then
    <     ViashDockerBuild $image_id $(ViashDockerBuildArgs "$engine_id")
    <   elif [[ "$setup_strategy" =~ ^ifneedbe ]]; then
    <     local save=$-; set +e
    <     ViashDockerLocalTagCheck $image_id
    <     local outCheck=$?
    <     [[ $save =~ e ]] && set -e
    <     if [ $outCheck -eq 0 ]; then
    <       ViashInfo "Image $image_id already exists"
    <     elif [ "$setup_strategy" == "ifneedbebuild" ]; then
    <       ViashDockerBuild $image_id --no-cache $(ViashDockerBuildArgs "$engine_id")
    <     elif [ "$setup_strategy" == "ifneedbecachedbuild" ]; then
    <       ViashDockerBuild $image_id $(ViashDockerBuildArgs "$engine_id")
    <     elif [ "$setup_strategy" == "ifneedbepull" ]; then
    <       ViashDockerPull $image_id
    <     elif [ "$setup_strategy" == "ifneedbepullelsebuild" ]; then
    <       ViashDockerPullElseBuild $image_id --no-cache $(ViashDockerBuildArgs "$engine_id")
    <     elif [ "$setup_strategy" == "ifneedbepullelsecachedbuild" ]; then
    <       ViashDockerPullElseBuild $image_id $(ViashDockerBuildArgs "$engine_id")
    <     else
    <       ViashError "Unrecognised Docker strategy: $setup_strategy"
    <       exit 1
    <     fi
    <   elif [ "$setup_strategy" == "push" -o "$setup_strategy" == "forcepush" -o "$setup_strategy" == "alwayspush" ]; then
    <     ViashDockerPush "$image_id"
    <   elif [ "$setup_strategy" == "pushifnotpresent" -o "$setup_strategy" == "gentlepush" -o "$setup_strategy" == "maybepush" ]; then
    <     local save=$-; set +e
    <     ViashDockerRemoteTagCheck $image_id
    <     local outCheck=$?
    <     [[ $save =~ e ]] && set -e
    <     if [ $outCheck -eq 0 ]; then
    <       ViashNotice "Container '$image_id' exists, doing nothing."
    <     else
    <       ViashNotice "Container '$image_id' does not yet exist."
    <       ViashDockerPush "$image_id"
    <     fi
    <   elif [ "$setup_strategy" == "donothing" -o "$setup_strategy" == "meh" ]; then
    <     ViashNotice "Skipping setup."
    <   else
    <     ViashError "Unrecognised Docker strategy: $setup_strategy"
    <     exit 1
    <   fi
    < }
    < 
    < # ViashDockerCheckCommands: Check whether a docker container has the required commands
    < #
    < # $1                  : image identifier with format `[registry/]image[:tag]`
    < # $@                  : commands to verify being present
    < # examples:
    < #   ViashDockerCheckCommands bash:3.2 bash ps foo
    < function ViashDockerCheckCommands {
    <   local image_id="$1"
    <   shift 1
    <   local commands="$@"
    <   local save=$-; set +e
    <   local missing # mark 'missing' as local in advance, otherwise the exit code of the command will be missing and always be '0'
    <   missing=$(docker run --rm --entrypoint=sh "$image_id" -c "for command in $commands; do command -v \$command >/dev/null 2>&1; if [ \$? -ne 0 ]; then echo \$command; exit 1; fi; done")
    <   local outCheck=$?
    <   [[ $save =~ e ]] && set -e
    <   if [ $outCheck -ne 0 ]; then
    <       ViashError "Docker container '$image_id' does not contain command '$missing'."
    <       exit 1
    <   fi
    < }
    < 
    < # ViashDockerBuild: build a docker image
    < # $1                               : image identifier with format `[registry/]image[:tag]`
    < # $...                             : additional arguments to pass to docker build
    < # $VIASH_META_TEMP_DIR             : temporary directory to store dockerfile & optional resources in
    < # $VIASH_META_NAME                 : name of the component
    < # $VIASH_META_RESOURCES_DIR        : directory containing the resources
    < # $VIASH_VERBOSITY                 : verbosity level
    < # exit code $?                     : whether or not the image was built successfully
    < function ViashDockerBuild {
    <   local image_id="$1"
    <   shift 1
    < 
    <   # create temporary directory to store dockerfile & optional resources in
    <   local tmpdir=$(mktemp -d "$VIASH_META_TEMP_DIR/dockerbuild-$VIASH_META_NAME-XXXXXX")
    <   local dockerfile="$tmpdir/Dockerfile"
    <   
    <   # Use a unique cleanup function name to avoid conflicts
    <   VIASH_DOCKER_BUILD_TMPDIR="$tmpdir"
    <   function ViashDockerBuildCleanup {
    <     rm -rf "$VIASH_DOCKER_BUILD_TMPDIR"
    <   }
    <   ViashRegisterCleanup ViashDockerBuildCleanup
    < 
    <   # store dockerfile and resources
    <   ViashDockerfile "$VIASH_ENGINE_ID" > "$dockerfile"
    < 
    <   # generate the build command
    <   local docker_build_cmd="docker build -t '$image_id' $@ '$VIASH_META_RESOURCES_DIR' -f '$dockerfile'"
    < 
    <   # build the container
    <   ViashNotice "Building container '$image_id' with Dockerfile"
    <   ViashInfo "$docker_build_cmd"
    <   local save=$-; set +e
    <   if [ $VIASH_VERBOSITY -ge $VIASH_LOGCODE_INFO ]; then
    <     eval $docker_build_cmd
    <   else
    <     eval $docker_build_cmd &> "$tmpdir/docker_build.log"
    <   fi
    < 
    <   # check exit code
    <   local out=$?
    <   [[ $save =~ e ]] && set -e
    <   if [ $out -ne 0 ]; then
    <     ViashError "Error occurred while building container '$image_id'"
    <     if [ $VIASH_VERBOSITY -lt $VIASH_LOGCODE_INFO ]; then
    <       ViashError "Transcript: --------------------------------"
    <       cat "$tmpdir/docker_build.log"
    <       ViashError "End of transcript --------------------------"
    <     fi
    <     exit 1
    <   fi
    < }
    < 
    < ######## End of helper functions for setting up Docker images for viash ########
    < 
    < # ViashDockerFile: print the dockerfile to stdout
    < # $1    : engine identifier
    < # return : dockerfile required to run this component
    < # examples:
    < #   ViashDockerFile
    < function ViashDockerfile {
    <   local engine_id="$1"
    < 
    <   if [[ "$engine_id" == "docker" ]]; then
    <     cat << 'VIASHDOCKER'
    < FROM bash:3.2
    < ENTRYPOINT []
    < LABEL org.opencontainers.image.description="Companion container for running component test_languages_bash"
    < LABEL org.opencontainers.image.created="2026-02-06T13:32:51+01:00"
    < LABEL org.opencontainers.image.version="test"
    < 
    < VIASHDOCKER
    <   fi
    < }
    < 
    < # ViashDockerBuildArgs: return the arguments to pass to docker build
    < # $1    : engine identifier
    < # return : arguments to pass to docker build
    < function ViashDockerBuildArgs {
    <   local engine_id="$1"
    < 
    <   if [[ "$engine_id" == "docker" ]]; then
    <     echo ""
    <   fi
    < }
    < 
    < # ViashAbsolutePath: generate absolute path from relative path
    < # borrowed from https://stackoverflow.com/a/21951256
    < # $1     : relative filename
    < # return : absolute path
    < # examples:
    < #   ViashAbsolutePath some_file.txt   # returns /path/to/some_file.txt
    < #   ViashAbsolutePath /foo/bar/..     # returns /foo
    < function ViashAbsolutePath {
    <   local thePath
    <   local parr
    <   local outp
    <   local len
    <   if [[ ! "$1" =~ ^/ ]]; then
    <     thePath="$PWD/$1"
    <   else
    <     thePath="$1"
    <   fi
    <   echo "$thePath" | (
    <     IFS=/
    <     read -a parr
    <     declare -a outp
    <     for i in "${parr[@]}"; do
    <       case "$i" in
    <       ''|.) continue ;;
    <       ..)
    <         len=${#outp[@]}
    <         if ((len==0)); then
    <           continue
    <         else
    <           unset outp[$((len-1))]
    <         fi
    <         ;;
    <       *)
    <         len=${#outp[@]}
    <         outp[$len]="$i"
    <       ;;
    <       esac
    <     done
    <     echo /"${outp[*]}"
    <   )
    < }
    < # ViashDockerAutodetectMount: auto configuring docker mounts from parameters
    < # $1                             : The parameter value
    < # returns                        : New parameter
    < # $VIASH_DIRECTORY_MOUNTS        : Added another parameter to be passed to docker
    < # $VIASH_DOCKER_AUTOMOUNT_PREFIX : The prefix to be used for the automounts
    < # examples:
    < #   ViashDockerAutodetectMount /path/to/bar      # returns '/viash_automount/path/to/bar'
    < #   ViashDockerAutodetectMountArg /path/to/bar   # returns '--volume="/path/to:/viash_automount/path/to"'
    < function ViashDockerAutodetectMount {
    <   local abs_path=$(ViashAbsolutePath "$1")
    <   local mount_source
    <   local base_name
    <   if [ -d "$abs_path" ]; then
    <     mount_source="$abs_path"
    <     base_name=""
    <   else
    <     mount_source=`dirname "$abs_path"`
    <     base_name=`basename "$abs_path"`
    <   fi
    <   local mount_target="$VIASH_DOCKER_AUTOMOUNT_PREFIX$mount_source"
    <   if [ -z "$base_name" ]; then
    <     echo "$mount_target"
    <   else
    <     echo "$mount_target/$base_name"
    <   fi
    < }
    < function ViashDockerAutodetectMountArg {
    <   local abs_path=$(ViashAbsolutePath "$1")
    <   local mount_source
    <   local base_name
    <   if [ -d "$abs_path" ]; then
    <     mount_source="$abs_path"
    <     base_name=""
    <   else
    <     mount_source=`dirname "$abs_path"`
    <     base_name=`basename "$abs_path"`
    <   fi
    <   local mount_target="$VIASH_DOCKER_AUTOMOUNT_PREFIX$mount_source"
    <   ViashDebug "ViashDockerAutodetectMountArg $1 -> $mount_source -> $mount_target"
    <   echo "--volume=\"$mount_source:$mount_target\""
    < }
    < function ViashDockerStripAutomount {
    <   local abs_path=$(ViashAbsolutePath "$1")
    <   echo "${abs_path#$VIASH_DOCKER_AUTOMOUNT_PREFIX}"
    < }
    < # initialise variables
    < VIASH_DIRECTORY_MOUNTS=()
    < 
    < # configure default docker automount prefix if it is unset
    < if [ -z "${VIASH_DOCKER_AUTOMOUNT_PREFIX+x}" ]; then
    <   VIASH_DOCKER_AUTOMOUNT_PREFIX="/viash_automount"
    < fi
    < 
    < # initialise docker variables
    < VIASH_DOCKER_RUN_ARGS=(-i --rm)
    ---
    > VIASH_ENGINE_ID='native'
    876c486
    <   echo "/home/rcannood/workspace/viash-io/viash/debug/test/viash_test_test_languages_bash_7237834006593708911"
    ---
    >   echo "/home/rcannood/workspace/viash-io/viash/debug/test_native/viash_test_test_languages_bash_3464054071439338915"
    886,898d495
    <   echo "Viash built in Docker:"
    <   echo "    ---setup=STRATEGY"
    <   echo "        Setup the docker container. Options are: alwaysbuild, alwayscachedbuild, ifneedbebuild, ifneedbecachedbuild, alwayspull, alwayspullelsebuild, alwayspullelsecachedbuild, ifneedbepull, ifneedbepullelsebuild, ifneedbepullelsecachedbuild, push, pushifnotpresent, donothing."
    <   echo "        Default: ifneedbepullelsecachedbuild"
    <   echo "    ---dockerfile"
    <   echo "        Print the dockerfile to stdout."
    <   echo "    ---docker_run_args=ARG"
    <   echo "        Provide runtime arguments to Docker. See the documentation on \`docker run\` for more information."
    <   echo "    ---docker_image_id"
    <   echo "        Print the docker image id to stdout."
    <   echo "    ---debug"
    <   echo "        Enter the docker container for debugging purposes."
    <   echo ""
    901,902c498,499
    <   echo "        Specify the engine to use. Options are: docker."
    <   echo "        Default: docker"
    ---
    >   echo "        Specify the engine to use. Options are: native."
    >   echo "        Default: native"
    939,968d535
    <         ---setup)
    <             VIASH_MODE='setup'
    <             VIASH_SETUP_STRATEGY="$2"
    <             shift 2
    <             ;;
    <         ---setup=*)
    <             VIASH_MODE='setup'
    <             VIASH_SETUP_STRATEGY="$(ViashRemoveFlags "$1")"
    <             shift 1
    <             ;;
    <         ---dockerfile)
    <             VIASH_MODE='dockerfile'
    <             shift 1
    <             ;;
    <         ---docker_run_args)
    <             VIASH_DOCKER_RUN_ARGS+=("$2")
    <             shift 2
    <             ;;
    <         ---docker_run_args=*)
    <             VIASH_DOCKER_RUN_ARGS+=("$(ViashRemoveFlags "$1")")
    <             shift 1
    <             ;;
    <         ---docker_image_id)
    <             VIASH_MODE='docker_image_id'
    <             shift 1
    <             ;;
    <         ---debug)
    <             VIASH_MODE='debug'
    <             shift 1
    <             ;;
    1000,1001c567,568
    < if   [ "$VIASH_ENGINE_ID" == "docker" ]  ; then
    <   VIASH_ENGINE_TYPE='docker'
    ---
    > if   [ "$VIASH_ENGINE_ID" == "native" ]  ; then
    >   VIASH_ENGINE_TYPE='native'
    1003c570
    <   ViashError "Engine '$VIASH_ENGINE_ID' is not recognized. Options are: docker."
    ---
    >   ViashError "Engine '$VIASH_ENGINE_ID' is not recognized. Options are: native."
    1007,1043d573
    < if [[ "$VIASH_ENGINE_TYPE" == "docker" ]]; then
    <   # check if docker is installed properly
    <   ViashDockerInstallationCheck
    < 
    <   # determine docker image id
    <   if [[ "$VIASH_ENGINE_ID" == 'docker' ]]; then
    <     VIASH_DOCKER_IMAGE_ID='test_languages_bash:test'
    <   fi
    < 
    <   # print dockerfile
    <   if [ "$VIASH_MODE" == "dockerfile" ]; then
    <     ViashDockerfile "$VIASH_ENGINE_ID"
    <     exit 0
    < 
    <   elif [ "$VIASH_MODE" == "docker_image_id" ]; then
    <     echo "$VIASH_DOCKER_IMAGE_ID"
    <     exit 0
    <   
    <   # enter docker container
    <   elif [[ "$VIASH_MODE" == "debug" ]]; then
    <     VIASH_DEBUG_CMD="docker run --entrypoint=bash ${VIASH_DOCKER_RUN_ARGS[@]} -v '$(pwd)':/pwd --workdir /pwd -t $VIASH_DOCKER_IMAGE_ID"
    <     ViashNotice "+ $VIASH_DEBUG_CMD"
    <     eval $VIASH_DEBUG_CMD
    <     exit 
    < 
    <   # build docker image
    <   elif [ "$VIASH_MODE" == "setup" ]; then
    <     ViashDockerSetup "$VIASH_DOCKER_IMAGE_ID" "$VIASH_SETUP_STRATEGY"
    <     ViashDockerCheckCommands "$VIASH_DOCKER_IMAGE_ID" 'bash'
    <     exit 0
    <   fi
    < 
    <   # check if docker image exists
    <   ViashDockerSetup "$VIASH_DOCKER_IMAGE_ID" ifneedbepullelsecachedbuild
    <   ViashDockerCheckCommands "$VIASH_DOCKER_IMAGE_ID" 'bash'
    < fi
    < 
    1157c687
    <   VIASH_PAR_dir="/home/rcannood/workspace/viash-io/viash/debug/test/viash_test_test_languages_bash_7237834006593708911"
    ---
    >   VIASH_PAR_dir="/home/rcannood/workspace/viash-io/viash/debug/test_native/viash_test_test_languages_bash_3464054071439338915"
    1290,1343c820,825
    < if [[ "$VIASH_ENGINE_TYPE" == "docker" ]]; then
    <   # detect volumes from file arguments
    <   VIASH_CHOWN_VARS=()
    < if [ ! -z "$VIASH_PAR_dir" ]; then
    <   VIASH_DIRECTORY_MOUNTS+=( "$(ViashDockerAutodetectMountArg "$VIASH_PAR_dir")" )
    <   VIASH_PAR_dir=$(ViashDockerAutodetectMount "$VIASH_PAR_dir")
    <   VIASH_CHOWN_VARS+=( "$VIASH_PAR_dir" )
    < fi
    < if [ ! -z "$VIASH_META_RESOURCES_DIR" ]; then
    <   VIASH_DIRECTORY_MOUNTS+=( "$(ViashDockerAutodetectMountArg "$VIASH_META_RESOURCES_DIR")" )
    <   VIASH_META_RESOURCES_DIR=$(ViashDockerAutodetectMount "$VIASH_META_RESOURCES_DIR")
    < fi
    < if [ ! -z "$VIASH_META_EXECUTABLE" ]; then
    <   VIASH_DIRECTORY_MOUNTS+=( "$(ViashDockerAutodetectMountArg "$VIASH_META_EXECUTABLE")" )
    <   VIASH_META_EXECUTABLE=$(ViashDockerAutodetectMount "$VIASH_META_EXECUTABLE")
    < fi
    < if [ ! -z "$VIASH_META_CONFIG" ]; then
    <   VIASH_DIRECTORY_MOUNTS+=( "$(ViashDockerAutodetectMountArg "$VIASH_META_CONFIG")" )
    <   VIASH_META_CONFIG=$(ViashDockerAutodetectMount "$VIASH_META_CONFIG")
    < fi
    < if [ ! -z "$VIASH_META_TEMP_DIR" ]; then
    <   VIASH_DIRECTORY_MOUNTS+=( "$(ViashDockerAutodetectMountArg "$VIASH_META_TEMP_DIR")" )
    <   VIASH_META_TEMP_DIR=$(ViashDockerAutodetectMount "$VIASH_META_TEMP_DIR")
    < fi
    < 
    <   # Add viash work dir to mounts
    <   VIASH_DIRECTORY_MOUNTS+=( "$(ViashDockerAutodetectMountArg "$VIASH_WORK_DIR")" )
    <   VIASH_WORK_DIR=$(ViashDockerAutodetectMount "$VIASH_WORK_DIR")
    <   
    <   # get unique mounts
    <   VIASH_UNIQUE_MOUNTS=($(for val in "${VIASH_DIRECTORY_MOUNTS[@]}"; do echo "$val"; done | sort -u))
    < fi
    < 
    < if [[ "$VIASH_ENGINE_TYPE" == "docker" ]]; then
    <   # change file ownership
    <   function ViashPerformChown {
    <     if (( ${#VIASH_CHOWN_VARS[@]} )); then
    <       set +e
    <       VIASH_CHMOD_CMD="docker run --entrypoint=bash --rm ${VIASH_UNIQUE_MOUNTS[@]} $VIASH_DOCKER_IMAGE_ID -c 'chown $(id -u):$(id -g) --silent --recursive ${VIASH_CHOWN_VARS[@]}'"
    <       ViashDebug "+ $VIASH_CHMOD_CMD"
    <       eval $VIASH_CHMOD_CMD
    <       set -e
    <     fi
    <   }
    <   ViashRegisterCleanup ViashPerformChown
    < fi
    < 
    < if [[ "$VIASH_ENGINE_TYPE" == "docker" ]]; then
    <   # helper function for filling in extra docker args
    <   if [ ! -z "$VIASH_META_MEMORY_B" ]; then
    <     VIASH_DOCKER_RUN_ARGS+=("--memory=${VIASH_META_MEMORY_B}")
    <   fi
    <   if [ ! -z "$VIASH_META_CPUS" ]; then
    <     VIASH_DOCKER_RUN_ARGS+=("--cpus=${VIASH_META_CPUS}")
    ---
    > if  [ "$VIASH_ENGINE_ID" == "native" ]  ; then
    >   if [ "$VIASH_MODE" == "run" ]; then
    >     VIASH_RUN_CMD="bash"
    >   else
    >     ViashError "Engine '$VIASH_ENGINE_ID' does not support mode '$VIASH_MODE'."
    >     exit 1
    1347,1350d828
    < if [[ "$VIASH_ENGINE_TYPE" == "docker" ]]; then
    <   VIASH_RUN_CMD="docker run --entrypoint=bash ${VIASH_DOCKER_RUN_ARGS[@]} ${VIASH_UNIQUE_MOUNTS[@]} $VIASH_DOCKER_IMAGE_ID"
    < fi
    < 
    1873,1893d1350
    < 
    < 
    < if [[ "$VIASH_ENGINE_TYPE" == "docker" ]]; then
    <   # strip viash automount from file paths
    <   
    <   if [ ! -z "$VIASH_PAR_dir" ]; then
    <     VIASH_PAR_dir=$(ViashDockerStripAutomount "$VIASH_PAR_dir")
    <   fi
    <   if [ ! -z "$VIASH_META_RESOURCES_DIR" ]; then
    <     VIASH_META_RESOURCES_DIR=$(ViashDockerStripAutomount "$VIASH_META_RESOURCES_DIR")
    <   fi
    <   if [ ! -z "$VIASH_META_EXECUTABLE" ]; then
    <     VIASH_META_EXECUTABLE=$(ViashDockerStripAutomount "$VIASH_META_EXECUTABLE")
    <   fi
    <   if [ ! -z "$VIASH_META_CONFIG" ]; then
    <     VIASH_META_CONFIG=$(ViashDockerStripAutomount "$VIASH_META_CONFIG")
    <   fi
    <   if [ ! -z "$VIASH_META_TEMP_DIR" ]; then
    <     VIASH_META_TEMP_DIR=$(ViashDockerStripAutomount "$VIASH_META_TEMP_DIR")
    <   fi
    < fi

## Experiment with base image

The testbash component is using bash:3.2 as base image (which is the minimal requirement for Viash since it needs to work on Mac OS X). When I manually override the base image from bash:3.2 to bash:4.3, the test pass successfully. bash:4.2 fails.

* bash:3.2 (default)  → fails
* bash:4.2          → fails
* bash:4.3          → passes
