# Viash 0.x.x (yyyy-MM-dd): TODO Add title

TODO add summary

## NEW FEATURES

* `Nextflow` runner: specifying a non-existent argument as a hashmap key for `fromState` and `toState` now raises an error (PR #793). 

## BREAKING CHANGES

* `Nextflow` runner: remove deprecated `map`, `mapId`, `mapData`, `mapPassthrough` and `renameKeys` arguments (PR #792).

* `Nextflow` runner: remove helper functions: `setWorkflowArguments`, `getWorkflowArguments`, `strictMap`, `passthroughMap`, `passthroughFlatMap`,  `passthroughFilter`, `channelFromParams`, `runComponents` (PRs #792, #811).

* Remove deprecated functionality `functionality` and `platforms` (PR #832).

## BUG FIXES

* `NextflowRunner`: Automatically convert integers to doubles when argument type is `double` (port of PR #824, PR #825).

## MINOR FIXES

* `Executable`: Add more info to the --help (PR #802).
