
### VIASH START
par = {
  'input1': 1,
  'input2': 2,
  'output': 'output.txt'
}

### VIASH END

import logging
import sys

result = par['input1'] * par['input2']

print(result)

if par['output'] is not None:
	with open(par['output'], 'w') as file:
		file.write(f"input1: {par['input1']} input2: {par['input2']} result: {result}")
