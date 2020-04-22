#!/bin/bash

sbt 'set test in assembly := {}' assembly

JAVA="java -jar target/scala-2.12/viash-assembly-0.0.1.jar"
TEST=src/test/resources

$JAVA -f $TEST/testpython/functionality.yaml -p $TEST/testpython/platform_docker.yaml export -o output/testpython_docker
$JAVA -f $TEST/testpython/functionality.yaml -p $TEST/testpython/platform_native.yaml export -o output/testpython_native
$JAVA -f $TEST/testbash/functionality.yaml -p $TEST/testbash/platform_docker.yaml export -o output/testbash_docker
$JAVA -f $TEST/testbash/functionality.yaml -p $TEST/testbash/platform_native.yaml export -o output/testbash_native
