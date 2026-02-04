# Viash JSON Injection Refactoring Progress

**PR:** [#762 - Switch to arrays](https://github.com/viash-io/viash/pull/762)  
**Issue:** [#705](https://github.com/viash-io/viash/issues/705)  
**Last Updated:** 2026-02-04

## Overview

This PR refactors the Viash codebase to make argument value parsing and handling more error-proof. The main changes include:

1. **Switch from code injection to JSON-based parameter passing**: Instead of injecting argument values directly into the script code, values are stored in a JSON file (`params.json`) and the script reads them at runtime using a language-specific JSON parser.

2. **Build-time code injection**: The JSON parsing code is injected at build time instead of runtime, making it more reliable and easier to debug.

3. **Language-specific JSON parsers**: Each supported language now has its own JSON parser implementation that doesn't require external dependencies.

## Test Results Summary

**Current Status:** 18 passed, 37 failed out of 55 tests

### Language Status

| Language | Native Tests | Docker Tests | JSON Generation | Notes |
|----------|--------------|--------------|-----------------|-------|
| Python ✅ | N/A | All PASS (6/6) | FAIL (file path issue) | Working correctly |
| R | N/A | Multiple tests PASS, main FAIL | FAIL | jsonlite converts long to numeric |
| Bash ❌ | All FAIL (6/6) | All FAIL (6/6) | FAIL | JSON parser not working |
| Scala ❌ | N/A | All FAIL (6/6) | FAIL (parse error) | JSON parser issues |
| JavaScript ❌ | N/A | All FAIL (6/6) | FAIL (module not found) | Module export issues |
| C# ❌ | N/A | All FAIL (6/6) | FAIL (Dictionary access) | Type conversion issues |
| Executable ❌ | N/A | FAIL (1/1) | N/A | Needs investigation |

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
- [x] [src/main/resources/io/viash/languages/r/ViashParseJson.R](src/main/resources/io/viash/languages/r/ViashParseJson.R) - Uses jsonlite (long issue)
- [x] [src/main/resources/io/viash/languages/bash/ViashParseJson.sh](src/main/resources/io/viash/languages/bash/ViashParseJson.sh) - Custom parser (not working)
- [x] [src/main/resources/io/viash/languages/scala/ViashParseJson.scala](src/main/resources/io/viash/languages/scala/ViashParseJson.scala) - Custom parser (not working)
- [x] [src/main/resources/io/viash/languages/javascript/ViashParseJson.js](src/main/resources/io/viash/languages/javascript/ViashParseJson.js) - Uses Node fs/JSON (module issue)
- [x] [src/main/resources/io/viash/languages/csharp/ViashParseJson.csx](src/main/resources/io/viash/languages/csharp/ViashParseJson.csx) - Custom parser (type issues)

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

Each script class has been updated with `generateInjectionMods`:
- [x] [src/main/scala/io/viash/config/resources/PythonScript.scala](src/main/scala/io/viash/config/resources/PythonScript.scala) ✅
- [x] [src/main/scala/io/viash/config/resources/RScript.scala](src/main/scala/io/viash/config/resources/RScript.scala)
- [x] [src/main/scala/io/viash/config/resources/BashScript.scala](src/main/scala/io/viash/config/resources/BashScript.scala)
- [x] [src/main/scala/io/viash/config/resources/ScalaScript.scala](src/main/scala/io/viash/config/resources/ScalaScript.scala)
- [x] [src/main/scala/io/viash/config/resources/JavaScriptScript.scala](src/main/scala/io/viash/config/resources/JavaScriptScript.scala)
- [x] [src/main/scala/io/viash/config/resources/CSharpScript.scala](src/main/scala/io/viash/config/resources/CSharpScript.scala)
- [x] [src/main/scala/io/viash/config/resources/Script.scala](src/main/scala/io/viash/config/resources/Script.scala) - Base trait with `readWithInjection`

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

#### 1. Move `generateInjectionMods` from Script to Language
- [ ] Create `generateInjectionMods` method in `Language` trait
- [ ] Move implementation from each `*Script.scala` to corresponding `Language` object
- [ ] Update `Script` trait to use `language.generateInjectionMods()`
- [ ] This provides better separation of concerns

**Rationale:** The injection code is language-specific, not script-type specific. It belongs in the `Language` abstraction.

**Pattern for `generateInjectionMods`:**
```scala
def generateInjectionMods(argsMetaAndDeps: Map[String, List[Argument[_]]], config: Config): ScriptInjectionMods = {
  // 1. Include the JSON parser helper functions
  val helperFunctions = viashParseJsonCode.split("\n")
    .takeWhile(line => !line.contains("main execution marker"))
    .mkString("\n")
  
  // 2. Generate code to parse JSON and extract sections
  val parseCode = "val _viashJsonData = ViashJsonParser.parseJson()\n"
  
  // 3. Generate type-aware extraction code for each section and argument
  val extractionCode = argsMetaAndDeps.map { case (dest, args) =>
    val sectionCode = s"val $dest = _viashJsonData.getOrElse(\"$dest\", Map.empty[String, Any])"
    val conversionCode = args.map { arg =>
      arg match {
        case _: IntegerArgument => s"val ${arg.plainName}: Option[Int] = $dest.get(\"${arg.plainName}\").map(_.toString.toInt)"
        case _: LongArgument => s"val ${arg.plainName}: Option[Long] = $dest.get(\"${arg.plainName}\").map(_.toString.toLong)"
        case _: DoubleArgument => s"val ${arg.plainName}: Option[Double] = $dest.get(\"${arg.plainName}\").map(_.toString.toDouble)"
        case _: BooleanArgumentBase => s"val ${arg.plainName}: Option[Boolean] = $dest.get(\"${arg.plainName}\").map(_.toString.toBoolean)"
        case _ => s"val ${arg.plainName}: Option[String] = $dest.get(\"${arg.plainName}\").map(_.toString)"
      }
    }.mkString("\n")
    sectionCode + "\n" + conversionCode
  }.mkString("\n\n")
  
  ScriptInjectionMods(params = helperFunctions + "\n\n" + parseCode + extractionCode)
}
```

#### 2. Fix R Language - Long Type Handling
- [ ] Update `ViashRenderJson.sh` to output long values as quoted strings
- [ ] Update `RScript.generateInjectionMods` to generate type conversion code
- [ ] For each argument, generate appropriate conversion: `as.integer()`, `bit64::as.integer64()`, `as.numeric()`, etc.
- [ ] Handle `NULL` values gracefully
- [ ] Handle arrays/multiple values with proper type conversion

**Key insight:** The `generateInjectionMods` function has access to `argsMetaAndDeps: Map[String, List[Argument[_]]]` which contains type information for each argument. Use this to generate type-specific conversion code.

#### 3. Fix Bash JSON Parser
- [ ] Current `ViashParseJsonBash` function not working correctly
- [ ] Issue: Variables not being properly exported to script scope
- [ ] Debug the variable export mechanism
- [ ] Test with simple cases first, then complex (arrays, nested objects)

**Constraints:**
- Cannot use external dependencies (jq, etc.)
- Must work with bash 3.2+ (macOS default)
- Can make assumptions about JSON structure (generated by ViashRenderJson)

#### 4. Fix Scala JSON Parser  
- [ ] Custom JSON parser has issues with certain JSON structures
- [ ] Debug parse errors seen in tests
- [ ] Update `ScalaScript.generateInjectionMods` to generate type conversion code
- [ ] Long values stored as strings need `.toString.toLong` conversion
- [ ] Generate proper Scala types (`Option[Int]`, `Option[Long]`, `List[String]`, etc.)

**Constraints:**
- Cannot use external libraries (circe, etc.)
- Must be pure Scala script code
- Type conversions generated at build time based on argument definitions

#### 5. Fix JavaScript Module Loading
- [ ] Issue: `viashParseJson` function not found at runtime
- [ ] Check `require.main === module` pattern
- [ ] Remove `module.exports` from injected code
- [ ] Ensure function is available in global scope

#### 6. Fix C# Type Conversion
- [ ] Dictionary access issues with dynamic types
- [ ] Update `CSharpScript.generateInjectionMods` to generate type conversion code
- [ ] Cast from `object` to specific types based on argument definitions
- [ ] Long values stored as strings need `long.Parse()` conversion
- [ ] Generate proper C# types with null handling

**Example generated code:**
```csharp
var par = _viashJsonData.ContainsKey("par") ? (Dictionary<string, object>)_viashJsonData["par"] : new Dictionary<string, object>();
int? whole_number = par.ContainsKey("whole_number") ? Convert.ToInt32(par["whole_number"]) : null;
long? long_number = par.ContainsKey("long_number") ? long.Parse((string)par["long_number"]) : null;
```

---

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
