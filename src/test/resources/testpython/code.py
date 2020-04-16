
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

if par['log'] is not None:
	logging.basicConfig(filename=par['log'], level=logging.INFO)

logging.info('Parsed input arguments.')

if par['output'] is not None: 
	logging.info('Writing output to file')
	import json
	with open(par['output'], 'w') as f:
		json.dump(par, f)
else:
	logging.info('Printing output to console')
	for key in par.keys():
		print(key + ": " + str(par[key]))
