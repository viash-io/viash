### VIASH START
par = {
  'string': 'mystring',
  'real_number': 123.987654,
  'whole_number': 17,
  'truth': True
}
### VIASH END

for key in par.keys():
	print(key + ": \"" + str(par[key]) + "\"")
