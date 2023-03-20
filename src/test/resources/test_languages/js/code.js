
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
	if (Array.isArray(par[key]) && par[key].length == 0)
		outFun(`${key}: |empty array|`)
	else if (Array.isArray(par[key]))
		outFun(`${key}: |${par[key].join(':')}|`)
	else if (par[key] == undefined)
		outFun(`${key}: ||`)
	else
		outFun(`${key}: |${par[key]}|`)
}

fs.readFile(par['input'], 'utf8', function(err, data){
    outFun(`head of input: |${data.split('\n')[0]}|`)
})
fs.readFile(meta['resources_dir'] + '/resource1.txt', 'utf8', function(err, data){
    outFun(`head of resource1: |${data.split('\n')[0]}|`)
})

for (const key in meta) {
	if (meta[key] == undefined || String(meta[key]) == 'NaN')
		outFun(`meta_${key}: ||`)
	else
		outFun(`meta_${key}: |${meta[key]}|`)
}