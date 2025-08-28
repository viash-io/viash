#!/usr/bin/env dotnet script

using System;
using System.Collections.Generic;
using System.IO;
using System.Text.RegularExpressions;
using System.Globalization;

public static class ViashYamlParser
{
    /// <summary>
    /// Parse simple YAML into a C# dictionary.
    /// 
    /// This function reads YAML content and converts it into a C# Dictionary.
    /// Arrays are converted to C# arrays or lists.
    /// </summary>
    /// <param name="yamlContent">YAML content as string. If null, reads from stdin.</param>
    /// <returns>Dictionary containing the parsed YAML data</returns>
    /// 
    /// The YAML format expected is simple:
    ///   key: value
    ///   array_key:
    ///     - item1
    ///     - item2
    public static Dictionary<string, object> ParseYaml(string yamlContent = null)
    {
        if (yamlContent == null)
        {
            yamlContent = Console.In.ReadToEnd();
        }
        
        var result = new Dictionary<string, object>();
        var lines = yamlContent.Trim().Split('\n');
        int i = 0;
        
        while (i < lines.Length)
        {
            var line = lines[i].TrimEnd();
            
            // Skip empty lines and comments
            if (string.IsNullOrWhiteSpace(line) || line.TrimStart().StartsWith("#"))
            {
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
                    // This might be the start of an array
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
                        result[key] = arrayItems.ToArray();
                        i = j;
                        continue;
                    }
                    else
                    {
                        // Empty value
                        result[key] = null;
                        i++;
                        continue;
                    }
                }
                else
                {
                    result[key] = ParseValue(value);
                    i++;
                    continue;
                }
            }
            
            i++;
        }
        
        return result;
    }
    
    /// <summary>
    /// Parse a YAML value into appropriate C# type.
    /// </summary>
    /// <param name="value">Value to parse</param>
    /// <returns>Parsed value with appropriate C# type</returns>
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
            // Quoted string - unescape
            var unquoted = value.Substring(1, value.Length - 2);
            unquoted = unquoted.Replace("\\\"", "\"");
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
                return value; // fallback to string
        }
        else if (Regex.IsMatch(value, @"^-?\d*\.\d+$"))
        {
            // Double
            if (double.TryParse(value, NumberStyles.Float, CultureInfo.InvariantCulture, out double doubleResult))
                return doubleResult;
            else
                return value; // fallback to string
        }
        else
        {
            // Unquoted string
            return value;
        }
    }
}

// If run as script, parse YAML from stdin and print result
if (Args.Length == 0)
{
    var result = ViashYamlParser.ParseYaml();
    
    // Print in a format that can be used in C#
    Console.WriteLine("// Parsed YAML parameters:");
    foreach (var kvp in result)
    {
        var key = kvp.Key;
        var value = kvp.Value;
        
        if (value == null)
        {
            Console.WriteLine($"var {key} = (object)null;");
        }
        else if (value is bool boolValue)
        {
            Console.WriteLine($"var {key} = {boolValue.ToString().ToLower()};");
        }
        else if (value is int intValue)
        {
            Console.WriteLine($"var {key} = {intValue};");
        }
        else if (value is long longValue)
        {
            Console.WriteLine($"var {key} = {longValue}L;");
        }
        else if (value is double doubleValue)
        {
            Console.WriteLine($"var {key} = {doubleValue.ToString(CultureInfo.InvariantCulture)};");
        }
        else if (value is string stringValue)
        {
            Console.WriteLine($"var {key} = \"{stringValue.Replace("\"", "\\\"").Replace("\n", "\\n")}\";");
        }
        else if (value is object[] arrayValue)
        {
            var items = new List<string>();
            foreach (var item in arrayValue)
            {
                if (item is string strItem)
                    items.Add($"\"{strItem.Replace("\"", "\\\"").Replace("\n", "\\n")}\"");
                else if (item is bool boolItem)
                    items.Add(boolItem.ToString().ToLower());
                else
                    items.Add(item?.ToString() ?? "null");
            }
            Console.WriteLine($"var {key} = new object[] {{ {string.Join(", ", items)} }};");
        }
    }
}
