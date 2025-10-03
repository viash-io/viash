#!/usr/bin/env python3

"""
Unit test for viash_parse_yaml function
This test verifies that the Python YAML parser correctly parses YAML content
and returns appropriate Python data structures.
"""

import sys
import os

# Add the path to the YAML parser
parser_path = 'src/main/resources/io/viash/languages/python/ViashParseYaml.py'
with open(parser_path, 'r') as f:
    exec(f.read())

# Colors for test output
class Colors:
    RED = '\033[0;31m'
    GREEN = '\033[0;32m'
    YELLOW = '\033[1;33m'
    NC = '\033[0m'  # No Color

# Test counters
tests_passed = 0
tests_failed = 0

def run_test(test_name, expected, actual):
    """Run a test and print results"""
    global tests_passed, tests_failed
    
    print(f"Testing {test_name}... ", end="")
    
    if actual == expected:
        print(f"{Colors.GREEN}PASS{Colors.NC}")
        tests_passed += 1
    else:
        print(f"{Colors.RED}FAIL{Colors.NC}")
        print(f"  Expected: {repr(expected)}")
        print(f"  Actual:   {repr(actual)}")
        tests_failed += 1

def test_array(test_name, expected_array, actual_array):
    """Test array equality"""
    global tests_passed, tests_failed
    
    print(f"Testing {test_name}... ", end="")
    
    if actual_array == expected_array:
        print(f"{Colors.GREEN}PASS{Colors.NC}")
        tests_passed += 1
    else:
        print(f"{Colors.RED}FAIL{Colors.NC}")
        print(f"  Expected array: {expected_array}")
        print(f"  Actual array:   {actual_array}")
        tests_failed += 1

def main():
    """Run all tests"""
    print(f"{Colors.YELLOW}Running viash_parse_yaml Python unit tests...{Colors.NC}")
    print()

    # Test 1: Basic key-value pairs in sections
    print("=== Test 1: Basic key-value pairs ===")
    yaml_content = """
par:
  input: "/path/to/input.txt"
  number: 42
  flag: true
  empty_value: null
meta:
  name: "test_component"
  version: "1.0.0"
"""

    result = viash_parse_yaml(yaml_content)
    
    # Test the results
    run_test("par.input", "/path/to/input.txt", result.get('par', {}).get('input'))
    run_test("par.number", 42, result.get('par', {}).get('number'))
    run_test("par.flag", True, result.get('par', {}).get('flag'))
    run_test("par.empty_value", None, result.get('par', {}).get('empty_value'))
    run_test("meta.name", "test_component", result.get('meta', {}).get('name'))
    run_test("meta.version", "1.0.0", result.get('meta', {}).get('version'))
    print()

    # Test 2: Arrays in sections
    print("=== Test 2: Arrays ===")
    yaml_content = """
par:
  files:
    - "file1.txt"
    - "file2.txt" 
    - "file3.txt"
  numbers:
    - 1
    - 2
    - 3
"""

    result = viash_parse_yaml(yaml_content)
    
    test_array("par.files", ["file1.txt", "file2.txt", "file3.txt"], 
               result.get('par', {}).get('files', []))
    test_array("par.numbers", [1, 2, 3], 
               result.get('par', {}).get('numbers', []))
    print()

    # Test 3: Quoted strings and special characters  
    print("=== Test 3: Quoted strings ===")
    yaml_content = '''
par:
  quoted_string: "Hello \"World\""
  single_quoted: 'Single quotes'
  with_newline: "Line 1\\nLine 2"
'''

    result = viash_parse_yaml(yaml_content)
    
    run_test("par.quoted_string", 'Hello "World"', result.get('par', {}).get('quoted_string'))
    run_test("par.single_quoted", "Single quotes", result.get('par', {}).get('single_quoted'))
    run_test("par.with_newline", "Line 1\nLine 2", result.get('par', {}).get('with_newline'))
    print()

    # Test 4: Boolean and numeric types
    print("=== Test 4: Boolean and numeric types ===")
    yaml_content = """
par:
  bool_true: true
  bool_false: false
  integer: 123
  negative: -456
  float_val: 3.14
  negative_float: -2.5
"""

    result = viash_parse_yaml(yaml_content)
    
    run_test("par.bool_true", True, result.get('par', {}).get('bool_true'))
    run_test("par.bool_false", False, result.get('par', {}).get('bool_false'))
    run_test("par.integer", 123, result.get('par', {}).get('integer'))
    run_test("par.negative", -456, result.get('par', {}).get('negative'))
    run_test("par.float_val", 3.14, result.get('par', {}).get('float_val'))
    run_test("par.negative_float", -2.5, result.get('par', {}).get('negative_float'))
    print()

    # Test 5: Root level parsing (no sections)
    print("=== Test 5: Root level parsing ===")
    yaml_content = """
simple_key: simple_value
number_key: 789
bool_key: true
"""

    result = viash_parse_yaml(yaml_content)
    
    run_test("simple_key", "simple_value", result.get('simple_key'))
    run_test("number_key", 789, result.get('number_key'))
    run_test("bool_key", True, result.get('bool_key'))
    print()

    # Test 6: Edge cases
    print("=== Test 6: Edge cases ===")
    yaml_content = """
par:
  empty_string: ""
  zero: 0
  empty_array:
meta:
  empty_section:
"""

    result = viash_parse_yaml(yaml_content)
    
    run_test("par.empty_string", "", result.get('par', {}).get('empty_string'))
    run_test("par.zero", 0, result.get('par', {}).get('zero'))
    run_test("par.empty_array", None, result.get('par', {}).get('empty_array'))
    run_test("meta.empty_section", None, result.get('meta', {}).get('empty_section'))
    print()

    # Print summary
    total_tests = tests_passed + tests_failed
    print("=" * 50)
    print(f"Tests completed: {total_tests}")
    print(f"{Colors.GREEN}Tests passed: {tests_passed}{Colors.NC}")
    if tests_failed > 0:
        print(f"{Colors.RED}Tests failed: {tests_failed}{Colors.NC}")
        sys.exit(1)
    else:
        print(f"{Colors.GREEN}All tests passed!{Colors.NC}")

if __name__ == "__main__":
    main()
