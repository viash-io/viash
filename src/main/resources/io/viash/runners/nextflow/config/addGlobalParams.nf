def addGlobalArguments(config) {
  def localConfig = [
    "functionality" : [
      "argument_groups": [
        [
          "name": "Nextflow input-output arguments",
          "description": "Input/output parameters for Nextflow itself. Please note that both publishDir and publish_dir are supported but at least one has to be configured.",
          "arguments" : [
            [
              'name': '--publish_dir',
              'required': true,
              'type': 'string',
              'description': 'Path to an output directory.',
              'example': 'output/',
              'multiple': false
            ],
            [
              'name': '--param_list',
              'required': false,
              'type': 'string',
              'description': '''Allows inputting multiple parameter sets to initialise a Nextflow channel. A `param_list` can either be a list of maps, a csv file, a json file, a yaml file, or simply a yaml blob.
              |
              |* A list of maps (as-is) where the keys of each map corresponds to the arguments of the pipeline. Example: in a `nextflow.config` file: `param_list: [ ['id': 'foo', 'input': 'foo.txt'], ['id': 'bar', 'input': 'bar.txt'] ]`.
              |* A csv file should have column names which correspond to the different arguments of this pipeline. Example: `--param_list data.csv` with columns `id,input`.
              |* A json or a yaml file should be a list of maps, each of which has keys corresponding to the arguments of the pipeline. Example: `--param_list data.json` with contents `[ {'id': 'foo', 'input': 'foo.txt'}, {'id': 'bar', 'input': 'bar.txt'} ]`.
              |* A yaml blob can also be passed directly as a string. Example: `--param_list "[ {'id': 'foo', 'input': 'foo.txt'}, {'id': 'bar', 'input': 'bar.txt'} ]"`.
              |
              |When passing a csv, json or yaml file, relative path names are relativized to the location of the parameter file. No relativation is performed when `param_list` is a list of maps (as-is) or a yaml blob.'''.stripMargin(),
              'example': 'my_params.yaml',
              'multiple': false,
              'hidden': true
            ],
          ]
        ]
      ]
    ]
  ]

  return processConfig(_mergeMap(config, localConfig))
}

def _mergeMap(Map lhs, Map rhs) {
  return rhs.inject(lhs.clone()) { map, entry ->
    if (map[entry.key] instanceof Map && entry.value instanceof Map) {
      map[entry.key] = _mergeMap(map[entry.key], entry.value)
    } else if (map[entry.key] instanceof Collection && entry.value instanceof Collection) {
      map[entry.key] += entry.value
    } else {
      map[entry.key] = entry.value
    }
    return map
  }
}
