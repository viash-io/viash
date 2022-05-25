
### VIASH START
par = {
  'input1': 1,
  'input2': 2,
  'output': 'output.txt'
}

### VIASH END

import logging
import sys

# This is a fake WIP component where the functionality is not yet implemented and always just returns 38
# Why 38? Because it's a nice number.
# The component is disabled in the functionality so it should not appear during building/testing/...
result = 38

print(result)

if par['output'] is not None:
	with open(par['output'], 'w') as file:
		file.write(f"input1: {par['input1']} input2: {par['input2']} result: {result}")
