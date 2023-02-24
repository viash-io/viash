
### VIASH START
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
meta = {
  'resources_dir': '.'
}
### VIASH END

import logging
import sys

if par['log'] is not None:
	logging.basicConfig(filename=par['log'],level=logging.INFO)
else:
	logging.basicConfig(stream=sys.stdout,level=logging.INFO)

logging.info('Parsed input arguments.')

if par['output'] is not None:
    logging.info('Writing output to file')
    output_file = open(par['output'], 'w')
else:
    logging.info('Printing output to console')

def echo(s):
    if par['output'] is not None:
        output_file.write(s + "\n")
    else:
        print(s)

try:
    for key, value in par.items():
        if isinstance(value, list):
            if not value:
                echo(f"{key}: |empty array|")
            else:
                echo(f"{key}: |{','.join(value)}|")
        else:
            echo(f"{key}: |{value}|")

    for key, value in meta.items():
        echo(f"meta_{key}: |{value}|")
finally:
    if par['output'] is not None:
        output_file.close()