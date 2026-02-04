
// VIASH START
let par = {
  'input': 'input.txt',
  'real_number': 123.987654,
  'whole_number': 17,
  's': '...',
  'truth': true,
  'output': 'output.txt',
  'log': 'log.txt',
  'optional': 'help',
  'optional_with_default': 'me'
}
let meta = {
	'resources_dir': '.'
}
// VIASH END


// define helper functions
const fs = require('fs')

let logFun;
if (!par['log']) {
	logFun = function(out) {
	  console.log("INFO:" + out);
	}
} else {
	logFun = function(out) {
		fs.appendFileSync(par['log'], "INFO:" + out + "\n");
	}
}
let outFun;
if (!par['output']) {
	outFun = console.log
} else {
	outFun = function(out) {
		fs.appendFileSync(par['output'], out + "\n");
	}
}

// process parameters
logFun('Parsed input arguments.')

if (!par['output']) { 
	logFun('Printing output to console')
} else {
	logFun('Writing output to file')
}

for (const key in par) {
	if (Array.isArray(par[key]) && par[key].length == 0)
		outFun(`${key}: ||`)
	else if (Array.isArray(par[key]))
		outFun(`${key}: |${par[key].join(';')}|`)
	else if (par[key] == undefined)
		outFun(`${key}: ||`)
	else
		outFun(`${key}: |${par[key]}|`)
}

let inputData = fs.readFileSync(par['input'], 'utf8');
outFun(`head of input: |${inputData.split('\n')[0]}|`)
let resourceData = fs.readFileSync(meta['resources_dir'] + '/resource1.txt', 'utf8');
outFun(`head of resource1: |${resourceData.split('\n')[0]}|`)

for (const key in meta) {
	if (meta[key] == undefined || String(meta[key]) == 'NaN')
		outFun(`meta_${key}: ||`)
	else
		outFun(`meta_${key}: |${meta[key]}|`)
}