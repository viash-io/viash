#!/usr/bin/env dotnet script

/*
 * Unit test for ViashYamlParser.ParseYaml function
 * This test verifies that the C# YAML parser correctly parses YAML content
 * and returns appropriate C# data structures.
 */

using System;
using System.Collections.Generic;
using System.IO;
using System.Text.RegularExpressions;
using System.Globalization;
using System.Linq;

public static class ViashYamlParser
{
    public static Dictionary<string, object> ParseYaml(string yamlContent = null)
    {
        if (yamlContent == null)
        {
            yamlContent = Console.In.ReadToEnd();
        }
        
        var result = new Dictionary<string, object>();
        var lines = yamlContent.Trim().Split('\n');
        int i = 0;
        string currentSection = null;
        
        while (i < lines.Length)
        {
            var line = lines[i].TrimEnd();
            
            // Skip empty lines and comments
            if (string.IsNullOrWhiteSpace(line) || line.TrimStart().StartsWith("#"))
            {
                i++;
                continue;
            }
            
            // Check for top-level sections (section name followed by colon)
            var sectionMatch = Regex.Match(line, @"^([a-zA-Z_][a-zA-Z0-9_]*):\s*$");
            if (sectionMatch.Success)
            {
                currentSection = sectionMatch.Groups[1].Value;
                result[currentSection] = new Dictionary<string, object>();
                i++;
                continue;
            }
            
            // Check for key-value pairs
            var match = Regex.Match(line, @"^(\s*)([^:]+):\s*(.*)");
            if (match.Success)
            {
                var indent = match.Groups[1].Value;
                var key = match.Groups[2].Value.Trim();
                var value = match.Groups[3].Value.Trim();
                
                if (string.IsNullOrEmpty(value))
                {
                    // Look ahead to see if next lines are array items
                    int j = i + 1;
                    var arrayItems = new List<object>();
                    
                    while (j < lines.Length)
                    {
                        var nextLine = lines[j].TrimEnd();
                        
                        if (string.IsNullOrWhiteSpace(nextLine))
                        {
                            j++;
                            continue;
                        }
                        
                        // Check if it's an array item
                        var arrayMatch = Regex.Match(nextLine, @"^(\s*)-\s*(.*)");
                        if (arrayMatch.Success)
                        {
                            var itemIndent = arrayMatch.Groups[1].Value;
                            var itemValue = arrayMatch.Groups[2].Value.Trim();
                            
                            // Make sure it's indented more than the key
                            if (itemIndent.Length > indent.Length)
                            {
                                arrayItems.Add(ParseValue(itemValue));
                                j++;
                                continue;
                            }
                        }
                        break;
                    }
                    
                    if (arrayItems.Count > 0)
                    {
                        // Store the array in the current section or root
                        if (currentSection != null)
                        {
                            ((Dictionary<string, object>)result[currentSection])[key] = arrayItems.ToArray();
                        }
                        else
                        {
                            result[key] = arrayItems.ToArray();
                        }
                        i = j;
                        continue;
                    }
                    else
                    {
                        // Empty value
                        if (currentSection != null)
                        {
                            ((Dictionary<string, object>)result[currentSection])[key] = null;
                        }
                        else
                        {
                            result[key] = null;
                        }
                        i++;
                        continue;
                    }
                }
                else
                {
                    // Regular key-value pair - store in current section or root
                    var parsedValue = ParseValue(value);
                    if (currentSection != null)
                    {
                        ((Dictionary<string, object>)result[currentSection])[key] = parsedValue;
                    }
                    else
                    {
                        result[key] = parsedValue;
                    }
                    i++;
                    continue;
                }
            }
            
            i++;
        }
        
        return result;
    }
    
    private static object ParseValue(string value)
    {
        if (value == "null")
        {
            return null;
        }
        else if (value == "true")
        {
            return true;
        }
        else if (value == "false")
        {
            return false;
        }
        else if (Regex.IsMatch(value, @"^""(.*)""$"))
        {
            // Double quoted string - unescape
            var unquoted = value.Substring(1, value.Length - 2);
            unquoted = unquoted.Replace("\\\"", "\"");
            unquoted = unquoted.Replace("\\n", "\n");
            unquoted = unquoted.Replace("\\\\", "\\");
            return unquoted;
        }
        else if (Regex.IsMatch(value, @"^'(.*)'$"))
        {
            // Single quoted string - unescape
            var unquoted = value.Substring(1, value.Length - 2);
            unquoted = unquoted.Replace("\\'", "'");
            unquoted = unquoted.Replace("\\n", "\n");
            unquoted = unquoted.Replace("\\\\", "\\");
            return unquoted;
        }
        else if (Regex.IsMatch(value, @"^-?\d+$"))
        {
            // Integer
            if (int.TryParse(value, out int intResult))
                return intResult;
            else if (long.TryParse(value, out long longResult))
                return longResult;
            else
                return value;
        }
        else if (Regex.IsMatch(value, @"^-?\d*\.\d+$"))
        {
            // Double
            if (double.TryParse(value, NumberStyles.Float, CultureInfo.InvariantCulture, out double doubleResult))
                return doubleResult;
            else
                return value;
        }
        else
        {
            // Unquoted string
            return value;
        }
    }
}

// Colors for test output
public static class Colors
{
    public static readonly string RED = "\u001b[0;31m";
    public static readonly string GREEN = "\u001b[0;32m";
    public static readonly string YELLOW = "\u001b[1;33m";
    public static readonly string NC = "\u001b[0m"; // No Color
}

// Test counters
public static int testsPassd = 0;
public static int testsFald = 0;

public static void RunTest(string testName, object expected, object actual)
{
    Console.Write($"Testing {testName}... ");
    
    bool isEqual = false;
    
    if (expected == null && actual == null)
        isEqual = true;
    else if (expected != null && actual != null)
    {
        if (expected is Array expectedArray && actual is Array actualArray)
        {
            isEqual = expectedArray.Cast<object>().SequenceEqual(actualArray.Cast<object>());
        }
        else
        {
            isEqual = expected.Equals(actual);
        }
    }
    
    if (isEqual)
    {
        Console.WriteLine($"{Colors.GREEN}PASS{Colors.NC}");
        testsPassd++;
    }
    else
    {
        Console.WriteLine($"{Colors.RED}FAIL{Colors.NC}");
        Console.WriteLine($"  Expected: {expected ?? "null"}");
        Console.WriteLine($"  Actual:   {actual ?? "null"}");
        testsFald++;
    }
}

public static void TestArray(string testName, object[] expectedArray, object[] actualArray)
{
    Console.Write($"Testing {testName}... ");
    
    if (expectedArray == null && actualArray == null)
    {
        Console.WriteLine($"{Colors.GREEN}PASS{Colors.NC}");
        testsPassd++;
        return;
    }
    
    if (expectedArray != null && actualArray != null && expectedArray.SequenceEqual(actualArray))
    {
        Console.WriteLine($"{Colors.GREEN}PASS{Colors.NC}");
        testsPassd++;
    }
    else
    {
        Console.WriteLine($"{Colors.RED}FAIL{Colors.NC}");
        Console.WriteLine($"  Expected array: [{string.Join(", ", expectedArray ?? new object[0])}]");
        Console.WriteLine($"  Actual array:   [{string.Join(", ", actualArray ?? new object[0])}]");
        testsFald++;
    }
}

public static void Main()
{
    Console.WriteLine($"{Colors.YELLOW}Running ViashYamlParser C# unit tests...{Colors.NC}");
    Console.WriteLine();

    // Test 1: Basic key-value pairs in sections
    Console.WriteLine("=== Test 1: Basic key-value pairs ===");
    string yamlContent1 = @"
par:
  input: ""/path/to/input.txt""
  number: 42
  flag: true
  empty_value: null
meta:
  name: ""test_component""
  version: ""1.0.0""
";

    var result1 = ViashYamlParser.ParseYaml(yamlContent1);
    
    // Test the results
    var par1 = result1.ContainsKey("par") ? (Dictionary<string, object>)result1["par"] : new Dictionary<string, object>();
    var meta1 = result1.ContainsKey("meta") ? (Dictionary<string, object>)result1["meta"] : new Dictionary<string, object>();
    
    RunTest("par.input", "/path/to/input.txt", par1.ContainsKey("input") ? par1["input"] : null);
    RunTest("par.number", 42, par1.ContainsKey("number") ? par1["number"] : null);
    RunTest("par.flag", true, par1.ContainsKey("flag") ? par1["flag"] : null);
    RunTest("par.empty_value", null, par1.ContainsKey("empty_value") ? par1["empty_value"] : null);
    RunTest("meta.name", "test_component", meta1.ContainsKey("name") ? meta1["name"] : null);
    RunTest("meta.version", "1.0.0", meta1.ContainsKey("version") ? meta1["version"] : null);
    Console.WriteLine();

    // Test 2: Arrays in sections
    Console.WriteLine("=== Test 2: Arrays ===");
    string yamlContent2 = @"
par:
  files:
    - ""file1.txt""
    - ""file2.txt"" 
    - ""file3.txt""
  numbers:
    - 1
    - 2
    - 3
";

    var result2 = ViashYamlParser.ParseYaml(yamlContent2);
    var par2 = result2.ContainsKey("par") ? (Dictionary<string, object>)result2["par"] : new Dictionary<string, object>();
    
    TestArray("par.files", new object[] {"file1.txt", "file2.txt", "file3.txt"}, 
               par2.ContainsKey("files") ? (object[])par2["files"] : new object[0]);
    TestArray("par.numbers", new object[] {1, 2, 3}, 
               par2.ContainsKey("numbers") ? (object[])par2["numbers"] : new object[0]);
    Console.WriteLine();

    // Test 3: Quoted strings and special characters  
    Console.WriteLine("=== Test 3: Quoted strings ===");
    string yamlContent3 = @"
par:
  quoted_string: ""Hello \""World\""""
  single_quoted: ""Single quotes""
  with_newline: ""Line 1\nLine 2""
";

    var result3 = ViashYamlParser.ParseYaml(yamlContent3);
    var par3 = result3.ContainsKey("par") ? (Dictionary<string, object>)result3["par"] : new Dictionary<string, object>();
    
    RunTest("par.quoted_string", "Hello \"World\"", par3.ContainsKey("quoted_string") ? par3["quoted_string"] : null);
    RunTest("par.single_quoted", "Single quotes", par3.ContainsKey("single_quoted") ? par3["single_quoted"] : null);
    RunTest("par.with_newline", "Line 1\nLine 2", par3.ContainsKey("with_newline") ? par3["with_newline"] : null);
    Console.WriteLine();

    // Test 4: Boolean and numeric types
    Console.WriteLine("=== Test 4: Boolean and numeric types ===");
    string yamlContent4 = @"
par:
  bool_true: true
  bool_false: false
  integer: 123
  negative: -456
  float_val: 3.14
  negative_float: -2.5
";

    var result4 = ViashYamlParser.ParseYaml(yamlContent4);
    var par4 = result4.ContainsKey("par") ? (Dictionary<string, object>)result4["par"] : new Dictionary<string, object>();
    
    RunTest("par.bool_true", true, par4.ContainsKey("bool_true") ? par4["bool_true"] : null);
    RunTest("par.bool_false", false, par4.ContainsKey("bool_false") ? par4["bool_false"] : null);
    RunTest("par.integer", 123, par4.ContainsKey("integer") ? par4["integer"] : null);
    RunTest("par.negative", -456, par4.ContainsKey("negative") ? par4["negative"] : null);
    RunTest("par.float_val", 3.14, par4.ContainsKey("float_val") ? par4["float_val"] : null);
    RunTest("par.negative_float", -2.5, par4.ContainsKey("negative_float") ? par4["negative_float"] : null);
    Console.WriteLine();

    // Test 5: Root level parsing (no sections)
    Console.WriteLine("=== Test 5: Root level parsing ===");
    string yamlContent5 = @"
simple_key: simple_value
number_key: 789
bool_key: true
";

    var result5 = ViashYamlParser.ParseYaml(yamlContent5);
    
    RunTest("simple_key", "simple_value", result5.ContainsKey("simple_key") ? result5["simple_key"] : null);
    RunTest("number_key", 789, result5.ContainsKey("number_key") ? result5["number_key"] : null);
    RunTest("bool_key", true, result5.ContainsKey("bool_key") ? result5["bool_key"] : null);
    Console.WriteLine();

    // Test 6: Edge cases
    Console.WriteLine("=== Test 6: Edge cases ===");
    string yamlContent6 = @"
par:
  empty_string: """"
  zero: 0
  empty_array:
meta:
  empty_section:
";

    var result6 = ViashYamlParser.ParseYaml(yamlContent6);
    var par6 = result6.ContainsKey("par") ? (Dictionary<string, object>)result6["par"] : new Dictionary<string, object>();
    var meta6 = result6.ContainsKey("meta") ? (Dictionary<string, object>)result6["meta"] : new Dictionary<string, object>();
    
    RunTest("par.empty_string", "", par6.ContainsKey("empty_string") ? par6["empty_string"] : null);
    RunTest("par.zero", 0, par6.ContainsKey("zero") ? par6["zero"] : null);
    RunTest("par.empty_array", null, par6.ContainsKey("empty_array") ? par6["empty_array"] : null);
    RunTest("meta.empty_section", null, meta6.ContainsKey("empty_section") ? meta6["empty_section"] : null);
    Console.WriteLine();

    // Print summary
    int totalTests = testsPassd + testsFald;
    Console.WriteLine(new string('=', 50));
    Console.WriteLine($"Tests completed: {totalTests}");
    Console.WriteLine($"{Colors.GREEN}Tests passed: {testsPassd}{Colors.NC}");
    if (testsFald > 0)
    {
        Console.WriteLine($"{Colors.RED}Tests failed: {testsFald}{Colors.NC}");
        Environment.Exit(1);
    }
    else
    {
        Console.WriteLine($"{Colors.GREEN}All tests passed!{Colors.NC}");
    }
}

Main();
