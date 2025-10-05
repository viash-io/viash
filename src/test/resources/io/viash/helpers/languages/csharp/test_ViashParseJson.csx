#!/usr/bin/env dotnet script

/*
 * Unit test for ViashJsonParser.ParseJson function
 * This test verifies that the C# JSON parser correctly parses JSON content
 * and returns appropriate C# data structures.
 */

#load "../../../../../../../main/resources/io/viash/languages/csharp/ViashParseJson.csx"

using System;
using System.Collections.Generic;

// ANSI color codes
const string RED = "\x1b[31m";
const string GREEN = "\x1b[32m";
const string RESET = "\x1b[0m";

int testsPassed = 0;
int testsFailed = 0;

void TestEqual(string testName, object actual, object expected)
{
    bool passed = false;
    
    if (actual == null && expected == null)
    {
        passed = true;
    }
    else if (actual != null && expected != null)
    {
        if (actual is List<object> actualList && expected is List<object> expectedList)
        {
            passed = actualList.Count == expectedList.Count;
            if (passed)
            {
                for (int i = 0; i < actualList.Count; i++)
                {
                    if (!actualList[i].Equals(expectedList[i]))
                    {
                        passed = false;
                        break;
                    }
                }
            }
        }
        else
        {
            passed = actual.Equals(expected) || actual.ToString() == expected.ToString();
        }
    }
    
    if (passed)
    {
        Console.WriteLine($"PASS: {testName}");
        testsPassed++;
    }
    else
    {
        Console.WriteLine($"{RED}FAIL: {testName}{RESET}");
        Console.WriteLine($"  Expected: {expected}");
        Console.WriteLine($"  Got: {actual}");
        testsFailed++;
    }
}

void TestTrue(string testName, bool condition)
{
    if (condition)
    {
        Console.WriteLine($"PASS: {testName}");
        testsPassed++;
    }
    else
    {
        Console.WriteLine($"{RED}FAIL: {testName}{RESET}");
        testsFailed++;
    }
}

Console.WriteLine("Running viash_parse_json C# unit tests...");
Console.WriteLine();

// Test JSON content
string jsonContent = @"{
  ""par"": {
    ""input"": ""file.txt"",
    ""number"": 42,
    ""flag"": true,
    ""empty_value"": """",
    ""array_simple"": [""a"", ""b"", ""c""],
    ""array_numbers"": [1, 2, 3],
    ""array_mixed"": [""text"", 123, true],
    ""nested"": {
      ""level1"": {
        ""level2"": ""deep_value""
      }
    },
    ""path_with_spaces"": ""/path/with spaces/file.txt"",
    ""quotes"": ""He said \""hello\"""",
    ""newlines"": ""line1\nline2"",
    ""tabs"": ""col1\tcol2"",
    ""string"": ""text"",
    ""integer"": 42,
    ""float"": 3.14,
    ""bool_true"": true,
    ""bool_false"": false,
    ""null_value"": null,
    ""empty_string"": """",
    ""zero"": 0,
    ""empty_array"": [],
    ""empty_object"": {}
  },
  ""meta"": {
    ""name"": ""test_component"",
    ""version"": ""1.0.0""
  },
  ""simple_key"": ""simple_value"",
  ""number_key"": 123,
  ""bool_key"": true
}";

// Parse the JSON
var tempFile = Path.GetTempFileName();
File.WriteAllText(tempFile, jsonContent);

try
{
    var parsed = ViashJsonParser.ParseJson(tempFile);
    var par = parsed["par"] as Dictionary<string, object>;
    var meta = parsed["meta"] as Dictionary<string, object>;

// Test 1: Basic key-value pairs
Console.WriteLine("=== Test 1: Basic key-value pairs ===");
TestEqual("par.input", par["input"], "file.txt");
TestEqual("par.number", par["number"], 42);
TestEqual("par.flag", par["flag"], true);
TestEqual("par.empty_value", par["empty_value"], "");
TestEqual("meta.name", meta["name"], "test_component");
TestEqual("meta.version", meta["version"], "1.0.0");
Console.WriteLine();

// Test 2: Arrays
Console.WriteLine("=== Test 2: Arrays ===");
var arraySimple = par["array_simple"] as List<object>;
TestTrue("par.array_simple length", arraySimple.Count == 3);
TestEqual("par.array_simple[0]", arraySimple[0], "a");
TestEqual("par.array_simple[1]", arraySimple[1], "b");
TestEqual("par.array_simple[2]", arraySimple[2], "c");

var arrayNumbers = par["array_numbers"] as List<object>;
TestTrue("par.array_numbers length", arrayNumbers.Count == 3);
TestEqual("par.array_numbers[0]", arrayNumbers[0], 1);
TestEqual("par.array_numbers[1]", arrayNumbers[1], 2);
TestEqual("par.array_numbers[2]", arrayNumbers[2], 3);

var arrayMixed = par["array_mixed"] as List<object>;
TestTrue("par.array_mixed length", arrayMixed.Count == 3);
Console.WriteLine();

// Test 3: Nested structures
Console.WriteLine("=== Test 3: Nested structures ===");
var nested = par["nested"] as Dictionary<string, object>;
var level1 = nested["level1"] as Dictionary<string, object>;
TestEqual("par.nested.level1.level2", level1["level2"], "deep_value");
Console.WriteLine();

// Test 4: Quoted strings
Console.WriteLine("=== Test 4: Quoted strings ===");
TestEqual("par.path_with_spaces", par["path_with_spaces"], "/path/with spaces/file.txt");
TestEqual("par.quotes", par["quotes"], "He said \"hello\"");
TestEqual("par.newlines", par["newlines"], "line1\nline2");
TestEqual("par.tabs", par["tabs"], "col1\tcol2");
Console.WriteLine();

// Test 5: Type conversions
Console.WriteLine("=== Test 5: Type conversions ===");
TestTrue("par.string type", par["string"] is string);
TestTrue("par.integer type", par["integer"] is int);
TestTrue("par.float type", par["float"] is double);
TestTrue("par.bool_true type", par["bool_true"] is bool);
TestEqual("par.bool_false", par["bool_false"], false);
TestTrue("par.null_value", par["null_value"] == null);
Console.WriteLine();

// Test 6: Root-level values
Console.WriteLine("=== Test 6: Root-level values ===");
TestEqual("simple_key", parsed["simple_key"], "simple_value");
TestEqual("number_key", parsed["number_key"], 123);
TestEqual("bool_key", parsed["bool_key"], true);
Console.WriteLine();

// Test 7: Edge cases
Console.WriteLine("=== Test 7: Edge cases ===");
TestEqual("par.empty_string", par["empty_string"], "");
TestEqual("par.zero", par["zero"], 0);
var emptyArray = par["empty_array"] as List<object>;
TestTrue("par.empty_array length", emptyArray.Count == 0);
TestTrue("par.empty_object type", par["empty_object"] is Dictionary<string, object>);
Console.WriteLine();

// Summary
Console.WriteLine("==================================================");
Console.WriteLine($"Tests completed: {testsPassed + testsFailed}");
Console.WriteLine($"Tests passed: {testsPassed}");
if (testsFailed > 0)
{
    Console.WriteLine($"{RED}Tests failed: {testsFailed}{RESET}");
    File.Delete(tempFile);
    Environment.Exit(1);
}
else
{
    Console.WriteLine($"{GREEN}All tests passed!{RESET}");
    File.Delete(tempFile);
    Environment.Exit(0);
}
}
finally
{
    if (File.Exists(tempFile))
    {
        File.Delete(tempFile);
    }
}
