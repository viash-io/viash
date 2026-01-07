#!/usr/bin/env python3

def viash_parse_yaml(yaml_content=None):
    """
    Parse simple YAML into a Python dictionary.
    
    This function reads YAML content and converts it into a Python dictionary.
    Lists are converted to Python lists and nested objects to dictionaries.
    
    Args:
        yaml_content (str, optional): YAML content as string. If None, reads from stdin.
    
    Returns:
        dict: Parsed YAML as a Python dictionary
    
    The YAML format expected supports:
      key: value
      array_key:
        - item1
        - item2
      nested_key:
        subkey: value
        subkey2: value2
    """
    import sys
    import re
    
    if yaml_content is None:
        yaml_content = sys.stdin.read()
    
    result = {}
    lines = yaml_content.strip().split('\n')
    i = 0
    
    while i < len(lines):
        line = lines[i].rstrip()
        
        # Skip empty lines and comments
        if not line.strip() or line.strip().startswith('#'):
            i += 1
            continue
        
        # Check for key-value pairs
        match = re.match(r'^(\s*)([^:]+):\s*(.*)', line)
        if match:
            indent_str, key, value = match.groups()
            indent_level = len(indent_str)
            key = key.strip()
            value = value.strip()
            
            if not value:
                # This is a section header - parse nested content
                j = i + 1
                nested_dict = {}
                
                # Parse all following lines that are more indented
                while j < len(lines):
                    next_line = lines[j].rstrip()
                    if not next_line.strip():
                        j += 1
                        continue
                    
                    # Check if this is an array item
                    array_match = re.match(r'^(\s*)-\s*(.*)', next_line)
                    if array_match:
                        array_indent_str, item_value = array_match.groups()
                        array_indent_level = len(array_indent_str)
                        
                        if array_indent_level > indent_level:
                            # This is an array for this key
                            array_items = []
                            array_items.append(_parse_value(item_value.strip()))
                            j += 1
                            
                            # Continue parsing array items
                            while j < len(lines):
                                array_line = lines[j].rstrip()
                                if not array_line.strip():
                                    j += 1
                                    continue
                                
                                array_item_match = re.match(r'^(\s*)-\s*(.*)', array_line)
                                if array_item_match:
                                    item_indent_str, item_val = array_item_match.groups()
                                    if len(item_indent_str) == array_indent_level:
                                        array_items.append(_parse_value(item_val.strip()))
                                        j += 1
                                    else:
                                        break
                                else:
                                    break
                            
                            result[key] = array_items
                            i = j
                            break
                        else:
                            break
                    else:
                        # Check for nested key-value pairs
                        next_match = re.match(r'^(\s*)([^:]+):\s*(.*)', next_line)
                        if next_match:
                            next_indent_str, next_key, next_value = next_match.groups()
                            next_indent_level = len(next_indent_str)
                            
                            if next_indent_level > indent_level:
                                # This is nested content
                                next_key = next_key.strip()
                                next_value = next_value.strip()
                                
                                if not next_value:
                                    # This might be an array within the nested section
                                    k = j + 1
                                    nested_array_items = []
                                    
                                    while k < len(lines):
                                        array_line = lines[k].rstrip()
                                        if not array_line.strip():
                                            k += 1
                                            continue
                                        
                                        nested_array_match = re.match(r'^(\s*)-\s*(.*)', array_line)
                                        if nested_array_match:
                                            nested_item_indent_str, nested_item_val = nested_array_match.groups()
                                            if len(nested_item_indent_str) > next_indent_level:
                                                nested_array_items.append(_parse_value(nested_item_val.strip()))
                                                k += 1
                                            else:
                                                break
                                        else:
                                            break
                                    
                                    if nested_array_items:
                                        nested_dict[next_key] = nested_array_items
                                        j = k
                                    else:
                                        # Empty value, could be empty array or empty object
                                        # For now, treat as empty object unless there's array syntax following
                                        nested_dict[next_key] = None
                                        j += 1
                                else:
                                    nested_dict[next_key] = _parse_value(next_value)
                                    j += 1
                            else:
                                # This is at the same or less indentation - end of nested content
                                break
                        else:
                            break
                
                if nested_dict:
                    result[key] = nested_dict
                    i = j
                else:
                    # Empty section
                    result[key] = {}
                    i += 1
            else:
                result[key] = _parse_value(value)
                i += 1
        else:
            i += 1
    
    return result

def _parse_value(value):
    """Parse a YAML value into appropriate Python type"""
    if value == 'null' or value == '~' or value == '':
        return None
    elif value == 'true':
        return True
    elif value == 'false':
        return False
    elif value.startswith('"') and value.endswith('"'):
        # Remove quotes and process escape sequences
        unquoted = value[1:-1]
        unquoted = unquoted.replace('\\"', '"')
        unquoted = unquoted.replace('\\n', '\n')
        unquoted = unquoted.replace('\\\\', '\\')
        return unquoted
    elif value.startswith("'") and value.endswith("'"):
        # Remove quotes and process escape sequences  
        unquoted = value[1:-1]
        unquoted = unquoted.replace("\\'", "'")
        unquoted = unquoted.replace('\\n', '\n')
        unquoted = unquoted.replace('\\\\', '\\')
        return unquoted
    else:
        # Try to parse as number
        try:
            if '.' in value:
                return float(value)
            else:
                return int(value)
        except ValueError:
            return value  # Return as string if not a number
