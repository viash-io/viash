
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

for key in par.keys():
	print(key + ": " + str(par[key]))

