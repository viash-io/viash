const fs = require('fs');

/**
 * Parse JSON parameters file into a JavaScript object.
 * 
 * @param {string|null} jsonPath - Path to the JSON file. If null, reads from $VIASH_WORK_PARAMS environment variable.
 * @returns {Object} Parsed JSON data.
 */
function viashParseJson(jsonPath = null) {
  if (jsonPath === null) {
    jsonPath = process.env.VIASH_WORK_PARAMS;
    if (!jsonPath) {
      throw new Error("VIASH_WORK_PARAMS environment variable not set");
    }
  }
  
  if (!fs.existsSync(jsonPath)) {
    throw new Error(`Parameters file not found: ${jsonPath}`);
  }
  
  try {
    const jsonText = fs.readFileSync(jsonPath, 'utf8');
    return JSON.parse(jsonText);
  } catch (error) {
    throw new Error(`Error parsing JSON file: ${error.message}`);
  }
}

module.exports = { viashParseJson };
