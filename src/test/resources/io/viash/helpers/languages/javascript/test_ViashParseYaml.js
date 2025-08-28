#!/usr/bin/env node

/**
 * Unit test for viashParseYaml function
 * This test verifies that the JavaScript YAML parser correctly parses YAML content
 * and returns appropriate JavaScript data structures.
 */

const fs = require('fs');
const path = require('path');

// Load the YAML parser
const yamlParserPath = path.join(__dirname, '../../../../main/resources/io/viash/languages/javascript/ViashParseYaml.js');
const yamlParserCode = fs.readFileSync(yamlParserPath, 'utf8');

// Since the parser doesn't export when run as main, we need to evaluate it and extract the functions
eval(yamlParserCode);

// Colors for test output
const Colors = {
    RED: '\033[0;31m',
    GREEN: '\033[0;32m',
    YELLOW: '\033[1;33m',
    NC: '\033[0m'  // No Color
};

// Test counters
let testsP = 0;
let testsF = 0;

function runTest(testName, expected, actual) {
    process.stdout.write(`Testing ${testName}... `);
    
    if (JSON.stringify(actual) === JSON.stringify(expected)) {
        console.log(`${Colors.GREEN}PASS${Colors.NC}`);
        testsP++;
    } else {
        console.log(`${Colors.RED}FAIL${Colors.NC}`);
        console.log(`  Expected: ${JSON.stringify(expected)}`);
        console.log(`  Actual:   ${JSON.stringify(actual)}`);
        testsF++;
    }
}

function testArray(testName, expectedArray, actualArray) {
    process.stdout.write(`Testing ${testName}... `);
    
    if (JSON.stringify(actualArray) === JSON.stringify(expectedArray)) {
        console.log(`${Colors.GREEN}PASS${Colors.NC}`);
        testsP++;
    } else {
        console.log(`${Colors.RED}FAIL${Colors.NC}`);
        console.log(`  Expected array: ${JSON.stringify(expectedArray)}`);
        console.log(`  Actual array:   ${JSON.stringify(actualArray)}`);
        testsF++;
    }
}

function main() {
    console.log(`${Colors.YELLOW}Running viashParseYaml JavaScript unit tests...${Colors.NC}`);
    console.log('');

    // Test 1: Basic key-value pairs in sections
    console.log('=== Test 1: Basic key-value pairs ===');
    const yamlContent1 = `
par:
  input: "/path/to/input.txt"
  number: 42
  flag: true
  empty_value: null
meta:
  name: "test_component"
  version: "1.0.0"
`;

    const result1 = viashParseYaml(yamlContent1);
    
    // Test the results
    runTest('par.input', '/path/to/input.txt', result1.par?.input);
    runTest('par.number', 42, result1.par?.number);
    runTest('par.flag', true, result1.par?.flag);
    runTest('par.empty_value', null, result1.par?.empty_value);
    runTest('meta.name', 'test_component', result1.meta?.name);
    runTest('meta.version', '1.0.0', result1.meta?.version);
    console.log('');

    // Test 2: Arrays in sections
    console.log('=== Test 2: Arrays ===');
    const yamlContent2 = `
par:
  files:
    - "file1.txt"
    - "file2.txt" 
    - "file3.txt"
  numbers:
    - 1
    - 2
    - 3
`;

    const result2 = viashParseYaml(yamlContent2);
    
    testArray('par.files', ['file1.txt', 'file2.txt', 'file3.txt'], 
               result2.par?.files || []);
    testArray('par.numbers', [1, 2, 3], 
               result2.par?.numbers || []);
    console.log('');

    // Test 3: Quoted strings and special characters  
    console.log('=== Test 3: Quoted strings ===');
    const yamlContent3 = `
par:
  quoted_string: "Hello \\"World\\""
  single_quoted: 'Single quotes'
  with_newline: "Line 1\\nLine 2"
`;

    const result3 = viashParseYaml(yamlContent3);
    
    runTest('par.quoted_string', 'Hello "World"', result3.par?.quoted_string);
    runTest('par.single_quoted', 'Single quotes', result3.par?.single_quoted);
    runTest('par.with_newline', 'Line 1\nLine 2', result3.par?.with_newline);
    console.log('');

    // Test 4: Boolean and numeric types
    console.log('=== Test 4: Boolean and numeric types ===');
    const yamlContent4 = `
par:
  bool_true: true
  bool_false: false
  integer: 123
  negative: -456
  float_val: 3.14
  negative_float: -2.5
`;

    const result4 = viashParseYaml(yamlContent4);
    
    runTest('par.bool_true', true, result4.par?.bool_true);
    runTest('par.bool_false', false, result4.par?.bool_false);
    runTest('par.integer', 123, result4.par?.integer);
    runTest('par.negative', -456, result4.par?.negative);
    runTest('par.float_val', 3.14, result4.par?.float_val);
    runTest('par.negative_float', -2.5, result4.par?.negative_float);
    console.log('');

    // Test 5: Root level parsing (no sections)
    console.log('=== Test 5: Root level parsing ===');
    const yamlContent5 = `
simple_key: simple_value
number_key: 789
bool_key: true
`;

    const result5 = viashParseYaml(yamlContent5);
    
    runTest('simple_key', 'simple_value', result5.simple_key);
    runTest('number_key', 789, result5.number_key);
    runTest('bool_key', true, result5.bool_key);
    console.log('');

    // Test 6: Edge cases
    console.log('=== Test 6: Edge cases ===');
    const yamlContent6 = `
par:
  empty_string: ""
  zero: 0
  empty_array:
meta:
  empty_section:
`;

    const result6 = viashParseYaml(yamlContent6);
    
    runTest('par.empty_string', '', result6.par?.empty_string);
    runTest('par.zero', 0, result6.par?.zero);
    runTest('par.empty_array', [], result6.par?.empty_array || []);
    runTest('meta.empty_section', null, result6.meta?.empty_section);
    console.log('');

    // Print summary
    const totalTests = testsP + testsF;
    console.log('='.repeat(50));
    console.log(`Tests completed: ${totalTests}`);
    console.log(`${Colors.GREEN}Tests passed: ${testsP}${Colors.NC}`);
    if (testsF > 0) {
        console.log(`${Colors.RED}Tests failed: ${testsF}${Colors.NC}`);
        process.exit(1);
    } else {
        console.log(`${Colors.GREEN}All tests passed!${Colors.NC}`);
    }
}

main();
