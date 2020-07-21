# viash 0.2.0 (asap)

## NEW FEATURES
* Allow testing a component with the `viash test` functionality. Tests are executed in a temporary directory on the specified platform. The temporary directory contains all the resource and test files. 
* `viash --version`: Add flag for printing the version of viash.
* Allow fetching resources from URL (http:// and https://)
* Allow retrieving functionality and platform YAMLs from URL.
* For docker containers, autoresolve path names of files. Use `---v path:path` or `---volume path:path` to manually mount a specific folder.
* Implement parameter multiplicity. 
  Set `multiple: true` to denote an argument to have higher multiplicity. 
  Run `./cmd --foo one --foo two --foo three:four` in order for multiple values to be added to the same parameter list.

## MAJOR CHANGES
* Remove passthrough parameters.
* Since CLI generation is now performed in the outer script, `viash pimp` has been deprecated.	

## MINOR CHANGES
* `viash run` and `viash test`: Allow changing the temporary directory by defining `VIASH_TEMP` as a environment variable. Temporary directories are cleaned up after successful executions.
* `viash run` and `viash test`: Exit(1) when execution or test fails.
* `viash export`: Add -m flag for outputting metadata after export.

## BUG FIXES
* NXF: Correctly escape path variable when running NXF command.
* NXF: Surround parameters with quotes when running NXF command.

## INTERNAL CHANGES
* Move CLI from inner script to outer script.
* Renamed Target to Platform
* Renamed Environment to Requirements

# viash 0.1.0 (2020-05-14)
* MAJOR CHANGES: Refactoring of the Functionality class as discussed in VIP1 (#1). This has resulted in a lot of internal changes, but the changes with regard to the yaml definitions are relatively minor. See the section below for more info.
* MINOR CHANGES: Updated the functionality.yamls under `atoms/` and `src/test/` to reflect these aforementioned changes.
* BUG FIX: Do not quote passthrough flags.
* BUG FIX: Allow for spaces inside of Docker volume paths.
* DOCUMENTATION: Updated the README.md.
* DOCUMENTATION: Provide some small examples at `doc/examples`.
* MINOR CHANGES: Allow for bioconductor and other repositories in the R environment.
* MINOR CHANGES: Add support for pip versioning syntax.

## Changes to functionality.yaml
* ftype has been renamed to function_type. The value for this field is also being checked.
* platform has been removed.
* Instead, the first resource listed is expected to have `type: r_script`, `type: bash_script`, `type: python_script`, or `type: executable`. The other resources are expected to have `type: file` by default, and are left untouched by Viash.
* in the arguments, field `flagValue` has been removed. Instead, use `type: boolean_true` and `type: boolean_false` to achieve the same effect.

## Changes to platform_(docker/native).yaml
* The `r: packages:` field has been renamed to `r: cran:`.

# viash 0.0.1 (2020-05-05)
* Initial proof of concept.
