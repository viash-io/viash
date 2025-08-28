#!/usr/bin/env node

function viashParseYaml(yamlContent = null) {
    /**
     * Parse simple YAML into a JavaScript object.
     * 
     * This function reads YAML content and converts it into a JavaScript object.
     * Arrays are converted to JavaScript arrays.
     * 
     * @param {string|null} yamlContent - YAML content as string. If null, reads from stdin.
     * @returns {Object} Parsed YAML as a JavaScript object
     * 
     * The YAML format expected is simple:
     *   key: value
     *   array_key:
     *     - item1
     *     - item2
     */
    
    if (yamlContent === null) {
        // Read from stdin (synchronously)
        const fs = require('fs');
        yamlContent = fs.readFileSync(0, 'utf-8');
    }
    
    const result = {};
    const lines = yamlContent.trim().split('\n');
    let i = 0;
    
    while (i < lines.length) {
        const line = lines[i].trimEnd();
        
        // Skip empty lines and comments
        if (!line.trim() || line.trim().startsWith('#')) {
            i++;
            continue;
        }
        
        // Check for key-value pairs
        const match = line.match(/^(\s*)([^:]+):\s*(.*)/);
        if (match) {
            const [, indent, key, value] = match;
            const cleanKey = key.trim();
            const cleanValue = value.trim();
            
            if (!cleanValue) {
                // This might be the start of an array
                // Look ahead to see if next lines are array items
                let j = i + 1;
                const arrayItems = [];
                
                while (j < lines.length) {
                    const nextLine = lines[j].trimEnd();
                    
                    if (!nextLine.trim()) {
                        j++;
                        continue;
                    }
                    
                    // Check if it's an array item
                    const arrayMatch = nextLine.match(/^(\s*)-\s*(.*)/);
                    if (arrayMatch) {
                        const [, itemIndent, itemValue] = arrayMatch;
                        
                        // Make sure it's indented more than the key
                        if (itemIndent.length > indent.length) {
                            arrayItems.push(parseValue(itemValue.trim()));
                            j++;
                            continue;
                        }
                    }
                    break;
                }
                
                if (arrayItems.length > 0) {
                    result[cleanKey] = arrayItems;
                    i = j;
                    continue;
                } else {
                    // Empty value
                    result[cleanKey] = null;
                    i++;
                    continue;
                }
            } else {
                result[cleanKey] = parseValue(cleanValue);
                i++;
                continue;
            }
        }
        
        i++;
    }
    
    return result;
}

function parseValue(value) {
    /**
     * Parse a YAML value into appropriate JavaScript type.
     * @param {string} value - Value to parse
     * @returns {*} Parsed value with appropriate JavaScript type
     */
    
    if (value === 'null') {
        return null;
    } else if (value === 'true') {
        return true;
    } else if (value === 'false') {
        return false;
    } else if (value.match(/^"(.*)"$/)) {
        // Quoted string - unescape
        let unquoted = value.slice(1, -1);
        unquoted = unquoted.replace(/\\"/g, '"');
        unquoted = unquoted.replace(/\\n/g, '\n');
        unquoted = unquoted.replace(/\\\\/g, '\\');
        return unquoted;
    } else if (value.match(/^-?\d+$/)) {
        // Integer
        return parseInt(value, 10);
    } else if (value.match(/^-?\d*\.\d+$/)) {
        // Float
        return parseFloat(value);
    } else {
        // Unquoted string
        return value;
    }
}
