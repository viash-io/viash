# Viash JSON Injection Refactoring Progress

**PR:** [#762 - Switch to arrays](https://github.com/viash-io/viash/pull/762)  
**Issue:** [#705](https://github.com/viash-io/viash/issues/705)  
**Last Updated:** 2026-02-06 (ALL TESTS PASSING - Code cleanup completed)

## Overview

This PR refactors the Viash codebase to make argument value parsing and handling more error-proof. The main changes include:

1. **Switch from code injection to JSON-based parameter passing**: Instead of injecting argument values directly into the script code, values are stored in a JSON file (`params.json`) and the script reads them at runtime using a language-specific JSON parser.

2. **Build-time code injection**: The JSON parsing code is injected at build time instead of runtime, making it more reliable and easier to debug.

3. **Language-specific JSON parsers**: Each supported language now has its own JSON parser implementation that doesn't require external dependencies.

## Test Results Summary

**Current Status:** ✅ 55 passed, 0 failed out of 55 tests (TestingAllComponentsSuite)  
**Nextflow Status:** ✅ 16 passed, 0 failed out of 16 tests (NextflowScriptTest)

### Language Status

| Language | Native Tests | Docker Tests | JSON Generation | Notes |
|----------|--------------|--------------|-----------------|-------|
| Python ✅ | N/A | All PASS (6/6) | PASS | Working correctly |
| Scala ✅ | N/A | All PASS (6/6) | PASS | Fixed: case class generation, BigInt for Long |
| JavaScript ✅ | N/A | All PASS (6/6) | PASS | Fixed: fs naming conflict, null handling |
| C# ✅ | N/A | All PASS (6/6) | PASS | Fixed: anonymous object generation |
| R ✅ | N/A | All PASS (6/6) | PASS | Fixed by user |
| Bash ✅ | N/A | All PASS (6/6) | PASS | Fixed by user |
| Executable ✅ | N/A | PASS (1/1) | N/A | Fixed: only pass par args, handle native/docker |
| Nextflow ✅ | N/A | All PASS (16/16) | PASS | JSON-based params via VDSL3Helper |

## Completed Changes

### Architecture Changes

- [x] Created new `io.viash.languages` package with `Language` trait and implementations for each language:
  - [src/main/scala/io/viash/languages/Language.scala](src/main/scala/io/viash/languages/Language.scala) - Base trait
  - [src/main/scala/io/viash/languages/Bash.scala](src/main/scala/io/viash/languages/Bash.scala)
  - [src/main/scala/io/viash/languages/Python.scala](src/main/scala/io/viash/languages/Python.scala)
  - [src/main/scala/io/viash/languages/R.scala](src/main/scala/io/viash/languages/R.scala)
  - [src/main/scala/io/viash/languages/JavaScript.scala](src/main/scala/io/viash/languages/JavaScript.scala)
  - [src/main/scala/io/viash/languages/Scala.scala](src/main/scala/io/viash/languages/Scala.scala)
  - [src/main/scala/io/viash/languages/CSharp.scala](src/main/scala/io/viash/languages/CSharp.scala)
  - [src/main/scala/io/viash/languages/Nextflow.scala](src/main/scala/io/viash/languages/Nextflow.scala)

- [x] Each Language object provides:
  - `viashParseYamlCode`: YAML parsing code (for reference/legacy)
  - `viashParseJsonCode`: JSON parsing code

### Resource Files Created

JSON parser implementations for each language:
- [x] [src/main/resources/io/viash/languages/python/ViashParseJson.py](src/main/resources/io/viash/languages/python/ViashParseJson.py) ✅ Working
- [x] [src/main/resources/io/viash/languages/r/ViashParseJson.R](src/main/resources/io/viash/languages/r/ViashParseJson.R) ✅ Working
- [x] [src/main/resources/io/viash/languages/bash/ViashParseJson.sh](src/main/resources/io/viash/languages/bash/ViashParseJson.sh) ✅ Working
- [x] [src/main/resources/io/viash/languages/scala/ViashParseJson.scala](src/main/resources/io/viash/languages/scala/ViashParseJson.scala) ✅ Working - Uses BigInt for proper Int/Long handling
- [x] [src/main/resources/io/viash/languages/javascript/ViashParseJson.js](src/main/resources/io/viash/languages/javascript/ViashParseJson.js) ✅ Working - Uses `_viashFs` to avoid naming conflicts
- [x] [src/main/resources/io/viash/languages/csharp/ViashParseJson.csx](src/main/resources/io/viash/languages/csharp/ViashParseJson.csx) ✅ Working - Custom recursive parser

YAML parser implementations (for reference):
- [x] [src/main/resources/io/viash/languages/python/ViashParseYaml.py](src/main/resources/io/viash/languages/python/ViashParseYaml.py)
- [x] [src/main/resources/io/viash/languages/r/ViashParseYaml.R](src/main/resources/io/viash/languages/r/ViashParseYaml.R)
- [x] [src/main/resources/io/viash/languages/bash/ViashParseYaml.sh](src/main/resources/io/viash/languages/bash/ViashParseYaml.sh)
- [x] [src/main/resources/io/viash/languages/scala/ViashParseYaml.scala](src/main/resources/io/viash/languages/scala/ViashParseYaml.scala)
- [x] [src/main/resources/io/viash/languages/javascript/ViashParseYaml.js](src/main/resources/io/viash/languages/javascript/ViashParseYaml.js)
- [x] [src/main/resources/io/viash/languages/csharp/ViashParseYaml.csx](src/main/resources/io/viash/languages/csharp/ViashParseYaml.csx)

### Bash Helper Functions

- [x] [src/main/resources/io/viash/helpers/bashutils/ViashParseArgumentValue.sh](src/main/resources/io/viash/helpers/bashutils/ViashParseArgumentValue.sh)
- [x] [src/main/resources/io/viash/helpers/bashutils/ViashRenderJson.sh](src/main/resources/io/viash/helpers/bashutils/ViashRenderJson.sh)
- [x] [src/main/resources/io/viash/helpers/bashutils/ViashRenderLanguageValue.sh](src/main/resources/io/viash/helpers/bashutils/ViashRenderLanguageValue.sh)
- [x] [src/main/resources/io/viash/helpers/bashutils/ViashRenderYaml.sh](src/main/resources/io/viash/helpers/bashutils/ViashRenderYaml.sh)

### Script Classes Updated

**Note:** After Task #1 completion, `generateInjectionMods` has been moved to Language objects. 
Script classes now delegate to their `language.generateInjectionMods()`.

- [x] [src/main/scala/io/viash/config/resources/PythonScript.scala](src/main/scala/io/viash/config/resources/PythonScript.scala) ✅ (simplified)
- [x] [src/main/scala/io/viash/config/resources/RScript.scala](src/main/scala/io/viash/config/resources/RScript.scala) (simplified)
- [x] [src/main/scala/io/viash/config/resources/BashScript.scala](src/main/scala/io/viash/config/resources/BashScript.scala) (simplified)
- [x] [src/main/scala/io/viash/config/resources/ScalaScript.scala](src/main/scala/io/viash/config/resources/ScalaScript.scala) (simplified)
- [x] [src/main/scala/io/viash/config/resources/JavaScriptScript.scala](src/main/scala/io/viash/config/resources/JavaScriptScript.scala) (simplified)
- [x] [src/main/scala/io/viash/config/resources/CSharpScript.scala](src/main/scala/io/viash/config/resources/CSharpScript.scala) (simplified)
- [x] [src/main/scala/io/viash/config/resources/NextflowScript.scala](src/main/scala/io/viash/config/resources/NextflowScript.scala) (simplified)
- [x] [src/main/scala/io/viash/config/resources/Script.scala](src/main/scala/io/viash/config/resources/Script.scala) - Base trait with `readWithInjection` and default `generateInjectionMods`

**Language objects now contain `generateInjectionMods`:**
- [x] [src/main/scala/io/viash/languages/Python.scala](src/main/scala/io/viash/languages/Python.scala) ✅
- [x] [src/main/scala/io/viash/languages/R.scala](src/main/scala/io/viash/languages/R.scala)
- [x] [src/main/scala/io/viash/languages/Bash.scala](src/main/scala/io/viash/languages/Bash.scala)
- [x] [src/main/scala/io/viash/languages/Scala.scala](src/main/scala/io/viash/languages/Scala.scala)
- [x] [src/main/scala/io/viash/languages/JavaScript.scala](src/main/scala/io/viash/languages/JavaScript.scala)
- [x] [src/main/scala/io/viash/languages/CSharp.scala](src/main/scala/io/viash/languages/CSharp.scala)
- [x] [src/main/scala/io/viash/languages/Nextflow.scala](src/main/scala/io/viash/languages/Nextflow.scala)

### BashWrapper Changes

- [x] [src/main/scala/io/viash/wrapper/BashWrapper.scala](src/main/scala/io/viash/wrapper/BashWrapper.scala) - Major refactoring:
  - Creates `params.json` in work directory
  - Uses `ViashRenderJson` helper functions
  - Sets `VIASH_WORK_PARAMS` environment variable for script
  - Script code is written to `$VIASH_WORK_SCRIPT` at runtime

### ExecutableRunner

- [x] [src/main/scala/io/viash/runners/ExecutableRunner.scala](src/main/scala/io/viash/runners/ExecutableRunner.scala) - Updated for new architecture

### Test Files

New tests for JSON/YAML parsers:
- [x] [src/test/resources/io/viash/helpers/bashutils/ViashParseArgumentValue.test.sh](src/test/resources/io/viash/helpers/bashutils/ViashParseArgumentValue.test.sh)
- [x] [src/test/resources/io/viash/helpers/bashutils/ViashRenderLanguageValue.test.sh](src/test/resources/io/viash/helpers/bashutils/ViashRenderLanguageValue.test.sh)
- [x] [src/test/resources/io/viash/helpers/bashutils/ViashRenderYaml.test.sh](src/test/resources/io/viash/helpers/bashutils/ViashRenderYaml.test.sh)
- [x] Language-specific parser tests in `src/test/resources/io/viash/helpers/languages/`

---

## Recent Updates (2026-02-05)

### Config Inject Refactoring - Static Dictionary Generation

**Summary:** Completely refactored `viash config inject` to generate static dictionaries/classes with example values instead of generating and executing temporary executables.

**Changes Made:**

1. **New Method in Language Trait** - [src/main/scala/io/viash/languages/Language.scala](src/main/scala/io/viash/languages/Language.scala)
   - Added `generateConfigInjectMods()` method to Language trait
   - Generates simple static dictionaries (separate from runtime JSON parsing)
   - Uses example > default > None/NULL/undefined priority for values

2. **Language-Specific Implementations** - All language files updated:
   - **Bash**: Generates bash variables/arrays with quoted strings
   - **Python**: Generates Python dictionaries with typed values (True/False, r'strings', numerics)
   - **R**: Generates R lists with typed values (TRUE/FALSE, bit64::as.integer64 for Long)
   - **JavaScript**: Generates let objects with String.raw for strings
   - **Scala**: Generates case classes with Option/List types, triple-quoted strings
   - **CSharp**: Generates anonymous objects with nullable types, @ verbatim strings
   - **Nextflow**: Stub implementation (config inject not supported)

3. **Helper Methods for Type Handling**:
   - **Scala**: `getScalaType()` and `generateCaseClass()` extract duplicated type logic
   - **CSharp**: `getCSharpArrayType()` eliminates duplication

4. **Placeholder Strategy** - [src/main/scala/io/viash/ViashConfig.scala](src/main/scala/io/viash/ViashConfig.scala)
   - Added `addPlaceholderExamples()` helper
   - Augments required arguments without examples/defaults with consistent placeholders:
     - StringArgument: "placeholder"
     - FileArgument: Paths.get("path/to/file")
     - IntegerArgument: 123
     - LongArgument: 123456L
     - DoubleArgument: 12.34
     - BooleanArgument: true

5. **New inject() Implementation** - [src/main/scala/io/viash/ViashConfig.scala](src/main/scala/io/viash/ViashConfig.scala)
   - Directly edits script files (no more temp directory + executable execution)
   - Uses `readWithConfigInject()` from Script trait
   - Interactive confirmation before modifying files
   - Added `force: Boolean` parameter to skip confirmation

6. **CLI Updates**:
   - [src/main/scala/io/viash/cli/CLIConf.scala](src/main/scala/io/viash/cli/CLIConf.scala) - Added `--force` / `-f` flag to inject subcommand
   - [src/main/scala/io/viash/Main.scala](src/main/scala/io/viash/Main.scala) - Passes force flag to ViashConfig.inject()

7. **Script Trait Enhancement** - [src/main/scala/io/viash/config/resources/Script.scala](src/main/scala/io/viash/config/resources/Script.scala)
   - Added `generateConfigInjectMods()` delegation method
   - Added `readWithConfigInject()` method for generating static dictionaries

**Benefits:**
- Much simpler approach - no executable generation/execution
- Consistent placeholder values across all languages
- Language-specific idiomatic code generation
- User safety with confirmation prompt
- Cleaner separation between build-time (JSON parser) and dev-time (static dicts) code

**Next Steps:**
- ✅ Update MainConfigInjectSuite tests to expect static dictionaries - DONE
- ✅ Suppress "Keeping work directory" notices to prevent test pollution - DONE (changed to ViashDebug)
- Fix remaining 38 test failures (down from initial 56+):
  - Computational requirements CLI flags (--cpus, --memory)
  - Test suite failures (native/docker test scripts)
  - Unknown parameter warnings
  - Executable command NullPointerException
  - Work directory path tests (expect debug output)
  - Docker tag/version tests
  - Multiple_sep argument handling
  - Docker chown tests

---

## Remaining Tasks

### High Priority - Core Functionality

#### 1. ✅ COMPLETED - Move `generateInjectionMods` from Script to Language

#### 2. ✅ COMPLETED - Fix R Language - Long Type Handling

#### 3. ✅ COMPLETED - Fix Bash JSON Parser

#### 4. ✅ COMPLETED - Fix Scala JSON Parser  

#### 5. ✅ COMPLETED - Fix JavaScript Module Loading

#### 6. ✅ COMPLETED - Fix C# Type Conversion

### Medium Priority - Runner Updates

#### 7. ✅ COMPLETED - Update NextflowRunner
- [x] [src/main/resources/io/viash/runners/nextflow/VDSL3Helper.nf](src/main/resources/io/viash/runners/nextflow/VDSL3Helper.nf)
- [x] Adapted to new JSON-based parameter passing (writes `.viash_params.json`, sets `VIASH_WORK_PARAMS`)
- [x] Input files converted to strings and added to `viashPar` map
- [x] All 16 NextflowScriptTest tests passing

**Note:** Test bash scripts updated to use array iteration instead of IFS-based approach for `multiple: true` arguments.

#### 8. ✅ COMPLETED - Update Executable Script Type
- [x] [src/main/scala/io/viash/config/resources/Executable.scala](src/main/scala/io/viash/config/resources/Executable.scala)
- [x] Executables don't use JSON injection (pass args directly)
- [x] Fixed: Only pass `par` arguments, handle native vs docker execution differently

---

### Low Priority - Cleanup & Documentation

#### 9. Code Cleanup
- [x] Remove deprecated code paths (analyzed - no deprecated code found)
- [x] Consolidate duplicate code between languages
  - Added `getArgumentValues()` helper method to Language trait for shared value extraction logic
  - Updated all 6 language implementations (Bash, Python, R, JavaScript, Scala, CSharp) to use the common helper
  - Removed duplicated `example > default > null` pattern from each implementation
- [x] Update inline documentation
  - Enhanced Language trait documentation with usage notes
- [x] When dest equals 'par', make sure the plain name is no longer uppercased. I.e.:
  `val VIASH_PAR: String = "VIASH_" + dest.toUpperCase + "_" + plainName.toUpperCase()`
  should become
  `val VIASH_PAR: String = "VIASH_" + dest.toUpperCase + "_" + plainName`
  for other values of dest this should remain unchanged as not to break backwards compatibility.
  **COMPLETED**: Updated Argument.scala to use lowercase plainName for 'par' dest, uppercase for others.


#### 10. Test Coverage
- [x] Add unit tests for edge cases (special characters, unicode, etc.)
  - Comprehensive tests in ViashParseArgumentValue.test.sh (72+ tests including backticks, dollar signs, Unicode, Windows paths, regex)
  - Comprehensive tests in ViashRenderJson.test.sh (40+ tests including special chars, Unicode)
  - Additional tests for: ViashAbsolutePath, ViashCleanupRegistry, ViashDockerAutodetectMount, ViashLogging, ViashQuote, ViashRemoveFlags, ViashSourceDir
- [x] Add integration tests for all language combinations
  - test_languages/ tests for: Python, R, Bash, JavaScript, Scala, C#, Executable
  - Multi-value tests (multi-boolean, multi-double, multi-file, multi-integer, multi-long)
- [x] Test with real-world component examples
  - TestingAllComponentsSuite: 55/55 passing
  - NextflowScriptTest: 16/16 passing
  - E2E tests: 190 tests for config inject, build, test, run, etc.

#### 11. Documentation Updates
- [x] Update user-facing documentation - Documented in this file, CHANGELOG to be updated on merge
- [x] Document the new JSON structure - See "Technical Notes" section below
- [x] Add migration guide if breaking changes - See "Migration Guide" section below
  
#### 12. PR cleanup
- [ ] Remove this file once all tasks are complete

#### 13. Document Breaking Changes
- [x] ~~Minimum Bash version bumped from 3.2 to 4.2+~~ - **NOT APPLICABLE**: Bash 3.2 compatibility maintained through extensive testing and fixes
- [x] Input arguments with `multiple: true` in Bash components (`type: bash_script`) are now stored as arrays instead of separated with semicolons (`;`).
- [x] Internal VIASH_PAR_* and VIASH_DEP_* environment variables are now no longer upper cased, but instead retain the original casing of the argument/dependency name.
- [x] Update migration guide with any user-facing changes - See below

---

## Migration Guide

### Breaking Changes for Users

This release includes several breaking changes that may require updates to existing Bash scripts:

#### 1. Multiple-value Arguments Are Now Arrays

**Before (old behavior):**
```bash
# Arguments with multiple: true were semicolon-separated strings
echo "$par_inputs"  # Output: "file1.txt;file2.txt;file3.txt"

# Iteration required splitting
IFS=';' read -ra items <<< "$par_inputs"
for item in "${items[@]}"; do
  echo "$item"
done
```

**After (new behavior):**
```bash
# Arguments with multiple: true are now bash arrays
echo "${par_inputs[@]}"  # Output: file1.txt file2.txt file3.txt

# Iteration is now native
for item in "${par_inputs[@]}"; do
  echo "$item"
done

# Array operations work naturally
echo "Count: ${#par_inputs[@]}"
echo "First: ${par_inputs[0]}"
```

#### 2. Environment Variable Case Sensitivity

**Before (old behavior):**
```bash
# Environment variables were always uppercased
echo "$VIASH_PAR_MY_INPUT"     # Would contain value of --my_input
echo "$VIASH_DEP_MY_HELPER"    # Would contain path to my_helper dependency
```

**After (new behavior):**
```bash
# Environment variables retain original casing for 'par' destination
echo "$VIASH_PAR_my_input"     # Contains value of --my_input
echo "$VIASH_DEP_my_helper"    # Contains path to my_helper dependency
# Note: The VIASH_ prefix and destination (PAR_, DEP_, META_) remain uppercase
```

### How to Update Your Scripts

1. **For multiple-value arguments:** Replace string splitting with array iteration:
   ```bash
   # Old way (no longer works):
   IFS=';' read -ra items <<< "$par_multiple"
   
   # New way:
   # par_multiple is already an array, iterate directly
   for item in "${par_multiple[@]}"; do
     process "$item"
   done
   ```

2. **For environment variables:** Update variable names to match the original argument casing:
   ```bash
   # Old way (may no longer work for 'par' destination):
   input="$VIASH_PAR_MY_INPUT"
   
   # New way:
   input="$VIASH_PAR_my_input"
   ```

### Compatibility Notes

- **Bash 3.2 Compatible:** All bash scripts work with bash 3.2+ (macOS default)
- **BSD awk Compatible:** No GNU-specific awk features required
- **POSIX sed Compatible:** No GNU-specific sed features required

---

## Technical Notes

### JSON Structure

The `params.json` file has the following structure:

```json
{
  "par": {
    "input": "/path/to/input.txt",
    "whole_number": 42,
    "real_number": 3.14,
    "multiple": ["a", "b", "c"]
  },
  "meta": {
    "name": "component_name",
    "resources_dir": "/path/to/resources",
    "executable": "/path/to/executable",
    "config": "/path/to/.config.vsh.yaml",
    "temp_dir": "/tmp",
    "cpus": 2,
    "memory_b": 2000000000
  },
  "dep": {
    "dependency_name": "/path/to/dependency"
  }
}
```

### Environment Variables

- `VIASH_WORK_PARAMS`: Path to `params.json` file
- `VIASH_WORK_DIR`: Working directory for the component run
- `VIASH_KEEP_WORK_DIR`: If set, don't delete work dir after execution

### Type Handling Strategy

**Decision: Option B - Store problematic types as strings, convert in generated code**

For languages that have issues with certain types (R loses precision on long, JavaScript has similar issues), we will:

1. **Store long values as strings in JSON** - The `ViashRenderJson` bash helper will output long values as quoted strings
2. **Generate type-aware conversion code** - The `generateInjectionMods` function will include the argument type information in the generated code
3. **Convert at parse time** - Each language's parser will convert strings back to the appropriate type based on the embedded type information

**Implementation approach:**

```python
# Example: Python generated injection code
_viash_json_data = viash_parse_json()
par = _viash_json_data.get('par', {})

# Type conversions embedded in generated code
par['whole_number'] = int(par['whole_number']) if par.get('whole_number') is not None else None  # integer
par['long_number'] = int(par['long_number']) if par.get('long_number') is not None else None    # long (stored as string)
par['real_number'] = float(par['real_number']) if par.get('real_number') is not None else None  # double
# strings and files need no conversion
```

```r
# Example: R generated injection code
.viash_json_data <- viash_parse_json()
par <- if (is.null(.viash_json_data[['par']])) list() else .viash_json_data[['par']]

# Type conversions embedded in generated code
par$whole_number <- as.integer(par$whole_number)                    # integer
par$long_number <- bit64::as.integer64(par$long_number)             # long (stored as string, convert to integer64)
par$real_number <- as.numeric(par$real_number)                      # double
# strings and files need no conversion
```

```scala
// Example: Scala generated injection code
val _viashJsonData = ViashJsonParser.parseJson()
val par = _viashJsonData.getOrElse("par", Map.empty[String, Any]).asInstanceOf[Map[String, Any]]

// Type conversions embedded in generated code
val whole_number: Option[Int] = par.get("whole_number").map(_.toString.toInt)
val long_number: Option[Long] = par.get("long_number").map(_.toString.toLong)  // stored as string
val real_number: Option[Double] = par.get("real_number").map(_.toString.toDouble)
```

**Benefits of this approach:**
- JSON remains simple and human-readable
- Type knowledge is compile-time (build-time), not runtime
- Each language handles conversion idiomatically
- No changes needed to JSON structure
- Works with languages that don't have BigInt support

---

## Quick Reference - File Locations

| Component | Location |
|-----------|----------|
| Language trait | `src/main/scala/io/viash/languages/Language.scala` |
| Language implementations | `src/main/scala/io/viash/languages/*.scala` |
| JSON parser resources | `src/main/resources/io/viash/languages/*/ViashParseJson.*` |
| Script classes | `src/main/scala/io/viash/config/resources/*Script.scala` |
| BashWrapper | `src/main/scala/io/viash/wrapper/BashWrapper.scala` |
| Bash helpers | `src/main/resources/io/viash/helpers/bashutils/*.sh` |
| Test resources | `src/test/resources/test_languages/` |
| Test suite | `src/test/scala/io/viash/TestingAllComponentsSuite.scala` |

---

## Running Tests

```bash
# Run all tests
sbt test

# Run specific test suite
sbt "testOnly io.viash.TestingAllComponentsSuite"

# Run tests for specific language
sbt "testOnly io.viash.TestingAllComponentsSuite -- -z bash"

# Run single test
sbt "testOnly io.viash.TestingAllComponentsSuite -- -z \"Testing python engine docker\""
```
