import json
import sys

def viash_parse_json(json_path=None):
    """
    Parse JSON parameters file into a Python dictionary.
    
    Args:
        json_path: Path to the JSON file. If None, reads from $VIASH_WORK_PARAMS environment variable.
    
    Returns:
        Dictionary containing the parsed JSON data.
    """
    if json_path is None:
        import os
        json_path = os.environ.get('VIASH_WORK_PARAMS')
        if json_path is None:
            raise ValueError("VIASH_WORK_PARAMS environment variable not set")
    
    try:
        with open(json_path, 'r') as f:
            return json.load(f)
    except FileNotFoundError:
        raise FileNotFoundError(f"Parameters file not found: {json_path}")
    except json.JSONDecodeError as e:
        raise ValueError(f"Invalid JSON in parameters file: {e}")
