#!/usr/bin/env node

// Test suite for viash_parse_json JavaScript function
console.log("Running viash_parse_json JavaScript unit tests...\n");

const fs = require('fs');
const os = require('os');
const path = require('path');

// Load the parser - simple relative path from project root
const parserPath = 'src/main/resources/io/viash/languages/javascript/ViashParseJson.js';
const { viashParseJson } = require(path.resolve(parserPath));

// Test helper functions
const RED = "\x1b[31m";
const GREEN = "\x1b[32m";
const RESET = "\x1b[0m";

let testPassed = 0;
let testFailed = 0;

function testEqual(description, actual, expected) {
  const actualStr = JSON.stringify(actual);
  const expectedStr = JSON.stringify(expected);
  
  if (actualStr === expectedStr) {
    console.log(`${GREEN}PASS${RESET}: ${description}`);
    testPassed++;
    return true;
  } else {
    console.log(`${RED}FAIL${RESET}: ${description}`);
    console.log(`  Expected: ${expectedStr}`);
    console.log(`  Got: ${actualStr}`);
    testFailed++;
    return false;
  }
}

// Create test JSON file
const testJson = path.join(os.tmpdir(), 'test_viash_' + Date.now() + '.json');
const jsonContent = {
  "par": {
    "input": "file.txt",
    "number": 42,
    "flag": true,
    "empty_value": "",
    "array_simple": ["a", "b", "c"],
    "array_numbers": [1, 2, 3],
    "array_mixed": ["text", 123, true, null],
    "nested": {
      "level1": {
        "level2": "deep_value"
      }
    },
    "path_with_spaces": "/path/with spaces/file.txt",
    "quotes": "She said \"hello\"",
    "newlines": "line1\nline2",
    "tabs": "col1\tcol2",
    "string": "text",
    "integer": 123,
    "float": 3.14,
    "bool_true": true,
    "bool_false": false,
    "null_value": null,
    "empty_string": "",
    "zero": 0,
    "empty_array": [],
    "empty_object": {}
  },
  "meta": {
    "name": "test_component",
    "version": "1.0"
  },
  "simple_key": "value",
  "number_key": 99,
  "bool_key": false
};

fs.writeFileSync(testJson, JSON.stringify(jsonContent, null, 2));

// Set environment variable
process.env.VIASH_WORK_PARAMS = testJson;

// Parse the JSON
const data = viashParseJson();

console.log("=== Test 1: Basic key-value pairs ===");
testEqual("par.input", data.par.input, "file.txt");
testEqual("par.number", data.par.number, 42);
testEqual("par.flag", data.par.flag, true);
testEqual("par.empty_value", data.par.empty_value, "");
testEqual("meta.name", data.meta.name, "test_component");
testEqual("meta.version", data.meta.version, "1.0");

console.log("\n=== Test 2: Arrays ===");
testEqual("par.array_simple", data.par.array_simple, ["a", "b", "c"]);
testEqual("par.array_numbers", data.par.array_numbers, [1, 2, 3]);
testEqual("par.array_mixed.length", data.par.array_mixed.length, 4);

console.log("\n=== Test 3: Nested structures ===");
testEqual("par.nested.level1.level2", data.par.nested.level1.level2, "deep_value");

console.log("\n=== Test 4: Quoted strings ===");
testEqual("par.path_with_spaces", data.par.path_with_spaces, "/path/with spaces/file.txt");
testEqual("par.quotes", data.par.quotes, "She said \"hello\"");
testEqual("par.newlines", data.par.newlines, "line1\nline2");
testEqual("par.tabs", data.par.tabs, "col1\tcol2");

console.log("\n=== Test 5: Type conversions ===");
testEqual("par.string type", typeof data.par.string, "string");
testEqual("par.integer type", typeof data.par.integer, "number");
testEqual("par.float type", typeof data.par.float, "number");
testEqual("par.bool_true type", typeof data.par.bool_true, "boolean");
testEqual("par.bool_false", data.par.bool_false, false);
testEqual("par.null_value", data.par.null_value, null);

console.log("\n=== Test 6: Root-level values ===");
testEqual("simple_key", data.simple_key, "value");
testEqual("number_key", data.number_key, 99);
testEqual("bool_key", data.bool_key, false);

console.log("\n=== Test 7: Edge cases ===");
testEqual("par.empty_string", data.par.empty_string, "");
testEqual("par.zero", data.par.zero, 0);
testEqual("par.empty_array.length", data.par.empty_array.length, 0);
testEqual("par.empty_object type", typeof data.par.empty_object, "object");

// Clean up
fs.unlinkSync(testJson);

// Print summary
console.log("\n==================================================");
console.log(`Tests completed: ${testPassed + testFailed}`);
console.log(`Tests passed: ${testPassed}`);
if (testFailed > 0) {
  console.log(`${RED}Tests failed: ${testFailed}${RESET}`);
  process.exit(1);
} else {
  console.log(`${GREEN}All tests passed!${RESET}`);
}
