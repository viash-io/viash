# Viash JSON Injection Refactoring Progress

**PR:** [#762 - Switch to arrays](https://github.com/viash-io/viash/pull/762)  
**Issue:** [#705](https://github.com/viash-io/viash/issues/705)  
**Last Updated:** 2026-02-04 (All languages COMPLETED, only Executable failing)

## Overview

This PR refactors the Viash codebase to make argument value parsing and handling more error-proof. The main changes include:

1. **Switch from code injection to JSON-based parameter passing**: Instead of injecting argument values directly into the script code, values are stored in a JSON file (`params.json`) and the script reads them at runtime using a language-specific JSON parser.

2. **Build-time code injection**: The JSON parsing code is injected at build time instead of runtime, making it more reliable and easier to debug.

3. **Language-specific JSON parsers**: Each supported language now has its own JSON parser implementation that doesn't require external dependencies.

## Test Results Summary

**Current Status:** 54 passed, 1 failed out of 55 tests

### Language Status

| Language | Native Tests | Docker Tests | JSON Generation | Notes |
|----------|--------------|--------------|-----------------|-------|
| Python ✅ | N/A | All PASS (6/6) | PASS | Working correctly |
| Scala ✅ | N/A | All PASS (6/6) | PASS | Fixed: case class generation, BigInt for Long |
| JavaScript ✅ | N/A | All PASS (6/6) | PASS | Fixed: fs naming conflict, null handling |
| C# ✅ | N/A | All PASS (6/6) | PASS | Fixed: anonymous object generation |
| R ✅ | N/A | All PASS (6/6) | PASS | Fixed by user |
| Bash ✅ | N/A | All PASS (6/6) | PASS | Fixed by user |
| Executable ❌ | N/A | FAIL (1/1) | N/A | Only remaining failure |

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

## Remaining Tasks

### High Priority - Core Functionality

#### 1. ✅ COMPLETED - Move `generateInjectionMods` from Script to Language

#### 2. ✅ COMPLETED - Fix R Language - Long Type Handling

#### 3. ✅ COMPLETED - Fix Bash JSON Parser

#### 4. ✅ COMPLETED - Fix Scala JSON Parser  

#### 5. ✅ COMPLETED - Fix JavaScript Module Loading

#### 6. ✅ COMPLETED - Fix C# Type Conversion

### Medium Priority - Runner Updates

#### 7. Update NextflowRunner
- [ ] [src/main/scala/io/viash/runners/NextflowRunner.scala](src/main/scala/io/viash/runners/NextflowRunner.scala)
- [ ] [src/main/scala/io/viash/runners/nextflow/NextflowHelper.scala](src/main/scala/io/viash/runners/nextflow/NextflowHelper.scala)
- [ ] Adapt to new JSON-based parameter passing
- [ ] Ensure compatibility with Nextflow's data flow model
- [ ] Test with VDSL3 components

**Current State:** NextflowHelper still uses `readWithInjection` but may need updates for work directory handling.

#### 8. Update Executable Script Type
- [ ] [src/main/scala/io/viash/config/resources/Executable.scala](src/main/scala/io/viash/config/resources/Executable.scala)
- [ ] Currently failing tests
- [ ] Executables don't use JSON injection (pass args directly)
- [ ] Verify the current approach is correct

---

### Low Priority - Cleanup & Documentation

#### 9. Code Cleanup
- [ ] Remove deprecated code paths
- [ ] Consolidate duplicate code between languages
- [ ] Update inline documentation

#### 10. Test Coverage
- [ ] Add unit tests for edge cases (special characters, unicode, etc.)
- [ ] Add integration tests for all language combinations
- [ ] Test with real-world component examples

#### 11. Documentation Updates
- [ ] Update user-facing documentation
- [ ] Document the new JSON structure
- [ ] Add migration guide if breaking changes
  
#### 12. PR cleanup
- [ ] Remove this file once all tasks are complete

#### 13. Document Breaking Changes
- [ ] Document minimum Bash version bump from 3.2 to 4.2+ (required for `declare -g` in JSON parser)
- [ ] Document change in how `multiple: true` arguments are processed (now stored as proper arrays)
- [ ] Update migration guide with any user-facing changes

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
