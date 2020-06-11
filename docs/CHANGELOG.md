# viash 0.1.1

* TESTING: Implement `viash test` functionality.
* MINOR CHANGES `viash test`: Tests are now executed in the resources directory.
* MINOR CHANGES `viash test`: The path of the resources directory is printed if verbose.

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
