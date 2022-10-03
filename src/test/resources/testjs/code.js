
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
if (typeof par['log'] === 'undefined') {
	logFun = function(out) {
	    console.log("INFO:" + out);
	}
} else {
	logFun = function(out) {
		fs.appendFile(par['log'], "INFO:" + out + "\n", function (err) {
			if (err) throw err;
		});
	}
}
let outFun;
if (typeof par['output'] === 'undefined') {
	outFun = console.log
} else {
	outFun = function(out) {
		fs.appendFile(par['output'], out + "\n", function (err) {
			if (err) throw err;
		});
	}
}

// process parameters
logFun('Parsed input arguments.')

if (typeof par['output'] === 'undefined') { 
	logFun('Printing output to console')
} else {
	logFun('Writing output to file')
}

for (const key in par) {
	outFun(`${key}: |${par[key]}|`)
}
for (const key in meta) {
	outFun(`meta_${key}: |${meta[key]}|`)
}