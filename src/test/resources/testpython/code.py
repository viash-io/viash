
### PORTASH START
par = {
  'input': 'input.txt',
  'real_number': 123.987654,
  'whole_number': 17,
  's': '...',
  'truth': True,
  'output': 'output.txt',
  'log': 'log.txt',
  'optional': 'help',
  'optional_with_default': 'me'
}
### PORTASH END

import logging
import sys

if par['log'] is not None:
	logging.basicConfig(filename=par['log'],level=logging.INFO)
else:
	logging.basicConfig(stream=sys.stdout,level=logging.INFO)

logging.info('Parsed input arguments.')

if par['output'] is not None: 
	logging.info('Writing output to file')
	with open(par['output'], 'w') as f:
		for key in par.keys():
			f.write(key + ": \"" + str(par[key]) + "\"\n")
else:
	logging.info('Printing output to console')
	for key in par.keys():
		print(key + ": \"" + str(par[key]) + "\"")
