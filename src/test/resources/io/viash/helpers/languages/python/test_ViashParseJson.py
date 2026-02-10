#!/usr/bin/env python3

"""
Unit test for viash_parse_json function
This test verifies that the Python JSON parser correctly parses JSON content
and returns appropriate Python data structures.
"""

import sys
import os
import tempfile
import json

# Add the path to the JSON parser
parser_path = 'src/main/resources/io/viash/languages/python/ViashParseJson.py'
with open(parser_path, 'r') as f:
    parser_code = f.read()
    # Extract just the function, not the main execution part
    exec('\n'.join([line for line in parser_code.split('\n') if not line.startswith('if __name__')]))

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

def test_with_temp_file(json_data):
    """Create a temp file with JSON data and parse it"""
    with tempfile.NamedTemporaryFile(mode='w', suffix='.json', delete=False) as f:
        json.dump(json_data, f)
        temp_path = f.name
    
    try:
        # Set environment variable
        old_env = os.environ.get('VIASH_WORK_PARAMS')
        os.environ['VIASH_WORK_PARAMS'] = temp_path
        result = viash_parse_json()
        if old_env is not None:
            os.environ['VIASH_WORK_PARAMS'] = old_env
        else:
            del os.environ['VIASH_WORK_PARAMS']
        return result
    finally:
        os.unlink(temp_path)

def main():
    """Run all tests"""
    print(f"{Colors.YELLOW}Running viash_parse_json Python unit tests...{Colors.NC}")
    print()

    # Test 1: Basic key-value pairs in sections
    print("=== Test 1: Basic key-value pairs ===")
    json_data = {
        "par": {
            "input": "/path/to/input.txt",
            "number": 42,
            "flag": True,
            "empty_value": None
        },
        "meta": {
            "name": "test_component",
            "version": "1.0.0"
        }
    }

    result = test_with_temp_file(json_data)
    
    # Test the results
    run_test("par.input", "/path/to/input.txt", result.get('par', {}).get('input'))
    run_test("par.number", 42, result.get('par', {}).get('number'))
    run_test("par.flag", True, result.get('par', {}).get('flag'))
    run_test("par.empty_value", None, result.get('par', {}).get('empty_value'))
    run_test("meta.name", "test_component", result.get('meta', {}).get('name'))
    run_test("meta.version", "1.0.0", result.get('meta', {}).get('version'))
    print()

    # Test 2: Arrays
    print("=== Test 2: Arrays ===")
    json_data = {
        "par": {
            "array_simple": ["a", "b", "c"],
            "array_numbers": [1, 2, 3],
            "array_mixed": ["string", 42, True, None]
        }
    }

    result = test_with_temp_file(json_data)
    
    run_test("par.array_simple", ["a", "b", "c"], result.get('par', {}).get('array_simple'))
    run_test("par.array_numbers", [1, 2, 3], result.get('par', {}).get('array_numbers'))
    run_test("par.array_mixed", ["string", 42, True, None], result.get('par', {}).get('array_mixed'))
    print()

    # Test 3: Nested structures
    print("=== Test 3: Nested structures ===")
    json_data = {
        "par": {
            "nested": {
                "level1": {
                    "level2": "deep_value"
                }
            }
        }
    }

    result = test_with_temp_file(json_data)
    
    run_test("par.nested.level1.level2", "deep_value", 
             result.get('par', {}).get('nested', {}).get('level1', {}).get('level2'))
    print()

    # Test 4: Quoted strings with special characters
    print("=== Test 4: Quoted strings ===")
    json_data = {
        "par": {
            "path_with_spaces": "/path/to/my file.txt",
            "quotes": 'Text with "quotes"',
            "newlines": "Line1\nLine2",
            "tabs": "Column1\tColumn2"
        }
    }

    result = test_with_temp_file(json_data)
    
    run_test("par.path_with_spaces", "/path/to/my file.txt", result.get('par', {}).get('path_with_spaces'))
    run_test("par.quotes", 'Text with "quotes"', result.get('par', {}).get('quotes'))
    run_test("par.newlines", "Line1\nLine2", result.get('par', {}).get('newlines'))
    run_test("par.tabs", "Column1\tColumn2", result.get('par', {}).get('tabs'))
    print()

    # Test 5: Type conversions
    print("=== Test 5: Type conversions ===")
    json_data = {
        "par": {
            "string": "hello",
            "integer": 123,
            "float": 3.14,
            "bool_true": True,
            "bool_false": False,
            "null_value": None
        }
    }

    result = test_with_temp_file(json_data)
    
    run_test("par.string type", str, type(result.get('par', {}).get('string')))
    run_test("par.integer type", int, type(result.get('par', {}).get('integer')))
    run_test("par.float type", float, type(result.get('par', {}).get('float')))
    run_test("par.bool_true type", bool, type(result.get('par', {}).get('bool_true')))
    run_test("par.bool_false", False, result.get('par', {}).get('bool_false'))
    run_test("par.null_value", None, result.get('par', {}).get('null_value'))
    print()

    # Test 6: Root-level values (no section)
    print("=== Test 6: Root-level values ===")
    json_data = {
        "simple_key": "simple_value",
        "number_key": 789,
        "bool_key": True
    }

    result = test_with_temp_file(json_data)
    
    run_test("simple_key", "simple_value", result.get('simple_key'))
    run_test("number_key", 789, result.get('number_key'))
    run_test("bool_key", True, result.get('bool_key'))
    print()

    # Test 7: Edge cases
    print("=== Test 7: Edge cases ===")
    json_data = {
        "par": {
            "empty_string": "",
            "zero": 0,
            "empty_array": [],
            "empty_object": {}
        }
    }

    result = test_with_temp_file(json_data)
    
    run_test("par.empty_string", "", result.get('par', {}).get('empty_string'))
    run_test("par.zero", 0, result.get('par', {}).get('zero'))
    run_test("par.empty_array", [], result.get('par', {}).get('empty_array'))
    run_test("par.empty_object", {}, result.get('par', {}).get('empty_object'))
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
