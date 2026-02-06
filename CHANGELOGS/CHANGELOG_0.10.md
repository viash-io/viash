# Viash 0.x.x (yyyy-MM-dd): TODO Add title

TODO add summary

## NEW FEATURES

* `Nextflow` runner: specifying a non-existent argument as a hashmap key for `fromState` and `toState` now raises an error (PR #793).

* `config run`: Add option to run a component using a package bundle downloaded from ViashHub (PR #816).
  This allows for running a component without having to build it first.
  Example: `viash run vsh://toolbox@v0.1.0/yq -- --input input.yaml --output output.yaml`.

* `Parameter passing`: Add support for unsetting argument values and computational requirements at runtime (PR #762, fixes #375).
  * Pass the literal `UNDEFINED` (unquoted) to set a single-value argument to undefined/null: `./my_component --arg UNDEFINED`
  * Pass `UNDEFINED_ITEM` as a value in a multi-value argument to represent a missing item: `./my_component --args "value1;UNDEFINED_ITEM;value3"`
  * Unset computational requirements with `---cpus UNDEFINED` or `---memory UNDEFINED`
  * Quote the values (`'"UNDEFINED"'` or `"'UNDEFINED'"`) to pass the literal string `UNDEFINED` instead of null.

## BREAKING CHANGES

* `Nextflow` runner: remove deprecated `map`, `mapId`, `mapData`, `mapPassthrough` and `renameKeys` arguments (PR #792).

* `Nextflow` runner: remove helper functions: `setWorkflowArguments`, `getWorkflowArguments`, `strictMap`, `passthroughMap`, `passthroughFlatMap`,  `passthroughFilter`, `channelFromParams`, `runComponents` (PRs #792, #811).

* Remove deprecated functionality `functionality` and `platforms` (PR #832).

* `Bash scripts`: Arguments with `multiple: true` are now stored as bash arrays instead of semicolon-separated strings (PR #762).
  Update scripts to use array syntax: `for item in "${par_inputs[@]}"; do ...` instead of IFS splitting.

## BUG FIXES

* `NextflowRunner`: Automatically convert integers to doubles when argument type is `double` (port of PR #824, PR #825).

* `Parameter passing`: Fix handling of special characters in argument values (PR #762, fixes #619, #705, #763, #821, #840).
  * Backticks in argument values no longer cause command substitution
  * Backslash-quote sequences (`\'`) no longer break Python syntax
  * Dollar signs, newlines, and other special characters are properly preserved

## MINOR FIXES

* `Executable`: Add more info to the --help (PR #802).

## INTERNAL CHANGES

* `Parameter passing`: Switch from code injection to JSON-based parameter passing (PR #762).
  Instead of injecting argument values directly into script code, values are now stored in a JSON file
  (`params.json`) and parsed at runtime using language-specific JSON parsers. This approach is more
  robust, easier to debug, and handles special characters (backticks, quotes, newlines) correctly.
