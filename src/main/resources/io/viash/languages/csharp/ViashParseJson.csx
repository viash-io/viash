/*
 * Parse JSON parameters file into a C# Dictionary
 */

using System;
using System.IO;
using System.Collections.Generic;
using System.Text.Json;

public static class ViashJsonParser
{
    /// <summary>
    /// Parse JSON parameters file into a C# Dictionary.
    /// </summary>
    /// <param name="jsonPath">Path to the JSON file. If null, reads from $VIASH_WORK_PARAMS environment variable.</param>
    /// <returns>Dictionary containing the parsed JSON data</returns>
    public static Dictionary<string, object> ParseJson(string jsonPath = null)
    {
        if (jsonPath == null)
        {
            jsonPath = Environment.GetEnvironmentVariable("VIASH_WORK_PARAMS");
            if (string.IsNullOrEmpty(jsonPath))
            {
                throw new InvalidOperationException("VIASH_WORK_PARAMS environment variable not set");
            }
        }
        
        if (!File.Exists(jsonPath))
        {
            throw new FileNotFoundException($"Parameters file not found: {jsonPath}");
        }
        
        try
        {
            string jsonText = File.ReadAllText(jsonPath);
            using var document = JsonDocument.Parse(jsonText);
            return ConvertJsonElement(document.RootElement) as Dictionary<string, object>;
        }
        catch (JsonException ex)
        {
            throw new InvalidOperationException($"Error parsing JSON file: {ex.Message}", ex);
        }
    }
    
    private static object ConvertJsonElement(JsonElement element)
    {
        switch (element.ValueKind)
        {
            case JsonValueKind.Object:
                var dict = new Dictionary<string, object>();
                foreach (var prop in element.EnumerateObject())
                {
                    dict[prop.Name] = ConvertJsonElement(prop.Value);
                }
                return dict;
                
            case JsonValueKind.Array:
                var list = new List<object>();
                foreach (var item in element.EnumerateArray())
                {
                    list.Add(ConvertJsonElement(item));
                }
                return list;
                
            case JsonValueKind.String:
                return element.GetString();
                
            case JsonValueKind.Number:
                if (element.TryGetInt32(out int intValue))
                    return intValue;
                return element.GetDouble();
                
            case JsonValueKind.True:
                return true;
                
            case JsonValueKind.False:
                return false;
                
            case JsonValueKind.Null:
                return null;
                
            default:
                return element.ToString();
        }
    }
}
