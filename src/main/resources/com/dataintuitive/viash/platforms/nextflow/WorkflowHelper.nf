/////////////////////////////////////
// Viash Workflow helper functions //
/////////////////////////////////////

import java.util.regex.Pattern
import java.io.BufferedReader
import java.io.FileReader
import java.nio.file.Paths
import groovy.json.JsonSlurper
import groovy.text.SimpleTemplateEngine
import org.yaml.snakeyaml.Yaml

def paramExists(name) {
  return params.containsKey(name) && params[name] != ""
}

def assertParamExists(name, description) {
  if (!paramExists(name)) {
    exit 1, "ERROR: Please provide a --${name} parameter ${description}"
  }
}

def getChild(parent, child) {
  if (child.contains("://") || Paths.get(child).isAbsolute()) {
    child
  } else {
    parent.replaceAll('/[^/]*$', "/") + child
  }
}

def readCsv(file) {
  def output = []
  def inputFile = file !instanceof File ? new File(file) : file

  // todo: allow escaped quotes in string
  // todo: allow single quotes?
  def splitRegex = Pattern.compile(''',(?=(?:[^"]*"[^"]*")*[^"]*$)''')
  def removeQuote = Pattern.compile('''"(.*)"''')

  def br = new BufferedReader(new FileReader(inputFile))

  def row = -1
  def header = null
  while (br.ready() && header == null) {
    def line = br.readLine()
    row++
    if (!line.startsWith("#")) {
      header = splitRegex.split(line).collect{field ->
        m = removeQuote.matcher(field)
        m.find() ? m.replaceFirst('$1') : field
      }
    }
  }
  assert header != null: "CSV file should contain a header"

  while (br.ready()) {
    def line = br.readLine()
    row++
    if (!line.startsWith("#")) {
      def data = splitRegex.split(line).collect{field ->
        m = removeQuote.matcher(field)
        m.find() ? m.replaceFirst('$1') : field
      }

      assert header.size() == data.size(): "Row $row should contain the same number as fields as the header"
      
      def dataMap = [header, data].transpose().collectEntries()
      output.add(dataMap)
    }
  }

  output
}

def readJsonBlob(str) {
  def jsonSlurper = new JsonSlurper()
  jsonSlurper.parseText(str)
}

def readJson(file) {
  def inputFile = file !instanceof File ? new File(file) : file
  def jsonSlurper = new JsonSlurper()
  jsonSlurper.parse(inputFile)
}

def readYamlBlob(str) {
  def yamlSlurper = new Yaml()
  yamlSlurper.load(str)
}

def readYaml(file) {
  def inputFile = file !instanceof File ? new File(file) : file
  def yamlSlurper = new Yaml()
  yamlSlurper.load(inputFile)
}

def processArgument(arg) {
  arg.multiple = arg.multiple ?: false
  arg.required = arg.required ?: false
  arg.direction = arg.direction ?: "input"
  arg.multiple_sep = arg.multiple_sep ?: ":"
  arg.plainName = arg.name.replaceAll("^-*", "")

  if (arg.type == "file" && arg.direction == "output") {
    def mult = arg.multiple ? "_*" : ""
    def extSearch = ""
    if (arg.default != null) {
      extSearch = arg.default
    } else if (arg.example != null) {
      extSearch = arg.example
    }
    if (extSearch instanceof List) {
      extSearch = extSearch[0]
    }
    def ext = extSearch.find("\\.[^\\.]+\$") ?: ""
    arg.default = "\$id.\$key.${arg.plainName}${mult}${ext}"
  }

  if (!arg.multiple) {
    if (arg.default != null && arg.default instanceof List) {
      arg.default = arg.default[0]
    }
    if (arg.example != null && arg.example instanceof List) {
      arg.example = arg.example[0]
    }
  }

  arg
}
def processConfig(config) {
  // assert .functionality etc.
  config.functionality.inputs = 
    (config.functionality.inputs ?: []).collect{arg ->
      arg.type = arg.type ?: "file"
      arg.direction = "input"
      processArgument(arg)
    }
  config.functionality.outputs = 
    (config.functionality.outputs ?: []).collect{arg ->
      arg.type = arg.type ?: "file"
      arg.direction = "output"
      processArgument(arg)
    }
  config.functionality.arguments = 
    (config.functionality.arguments ?: []).collect{arg ->
      processArgument(arg)
    }
  config.functionality.allArguments = 
    config.functionality.inputs +
    config.functionality.outputs +
    config.functionality.arguments
  config
}

def readConfig(file) {
  def config = readYaml(file)
  processConfig(config)
}


def mergeMap(Map lhs, Map rhs) {
  return rhs.inject(lhs.clone()) { map, entry ->
    if (map[entry.key] instanceof Map && entry.value instanceof Map) {
      map[entry.key] = mergeMap(map[entry.key], entry.value)
    } else if (map[entry.key] instanceof Collection && entry.value instanceof Collection) {
      map[entry.key] += entry.value
    } else {
      map[entry.key] = entry.value
    }
    return map
  }
}

def helpMessage(params, config) {
  if (paramExists("help")) {
    def localConfig = [
      "functionality" : [
        "arguments": [
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
            'description': '''Allows inputting multiple parameter sets to initialise a Nextflow channel. Possible formats are csv, json, yaml, or simply a yaml_blob.
            |A csv should have column names which correspond to the different arguments of this pipeline.
            |A json or a yaml file should be a list of maps, each of which has keys corresponding to the arguments of the pipeline.
            |A yaml blob can also be passed directly as a parameter.
            |Inside the Nextflow pipeline code, params.params_list can also be used to directly a list of parameter sets.
            |When passing a csv, json or yaml, relative path names are relativized to the location of the parameter file.'''.stripMargin(),
            'example': 'my_params.yaml',
            'multiple': false
          ],
          [
            'name': '--param_list_format',
            'required': false,
            'type': 'string',
            'description': 'Manually specify the param_list_format. Must be one of \'csv\', \'json\', \'yaml\', \'yaml_blob\', \'asis\' or \'none\'.',
            'example': 'yaml',
            'choices': ['csv', 'json', 'yaml', 'yaml_blob', 'asis', 'none'],
            'multiple': false
          ],
        ],
        "arguments_groups": [
          [
            "name": "Global",
            "description": "Nextflow-related arguments.",
            "arguments" : [ "publish_dir", "param_list", "param_list_format" ]
          ]
        ]
      ]
    ]
    def mergedConfig = processConfig(mergeMap(config, localConfig))

    def template = '''\
    |${functionality.name}
    |<%= (functionality.description) ? "\\n" + functionality.description.trim() : "<<REMOVE>>" %>
    |<% for (group in (functionality.arguments_groups.size() > 1 ) ? functionality.arguments_groups : [ [ name: "Options", arguments: functionality.allArguments.collect{ it.plainName } ] ] ) { %>
    |<%= group.name ? group.name + (group.name == "Options" ? ":" : " options:"): "" %>
    |<%= group.description ? "    " + group.description.trim().replaceAll("\\n", "\\n    ") + "\\n" : "<<REMOVE>>" %><% for (argument in functionality.allArguments) { %><% if (group.arguments.contains(argument.plainName)) { %>
    |    <%= "--" + argument.plainName %>
    |        type: <%= argument.type %><%= (argument.required) ? ", required parameter" : "" %><%= (argument.multiple) ? ", multiple values allowed" : "" %>
    |        <%= (argument.example)  ? "example: ${argument.example}" : "<<REMOVE>>" %>
    |        <%= (argument.default)  ? "default: ${argument.default}" : "<<REMOVE>>" %>
    |        <%= (argument.description) ? argument.description.trim().replaceAll("\\n", "\\n        ") : "<<REMOVE>>" %>
    |<% } } } %>
    '''.stripMargin()

    def engine = new groovy.text.SimpleTemplateEngine()
    def help = engine
        .createTemplate(template)
        .make(mergedConfig)
        .toString()
        .replaceAll("\s*<<REMOVE>>\n","")

    println(help)
    exit 0

  }
}

def guessMultiParamFormat(params) {
  if (!params.containsKey("param_list") || params.param_list == null) {
    "none"
  } else if (params.containsKey("multiParamsFormat")) {
    params.multiParamsFormat
  } else {
    def param_list = params.param_list

    if (param_list !instanceof String) {
      "asis"
    } else if (param_list.endsWith(".csv")) {
      "csv"
    } else if (param_list.endsWith(".json") || param_list.endsWith(".jsn")) {
      "json"
    } else if (param_list.endsWith(".yaml") || param_list.endsWith(".yml")) {
      "yaml"
    } else {
      "yaml_blob"
    }
  }
}

def paramsToList(params, config) {
  // fetch default params from functionality
  def defaultArgs = config.functionality.allArguments
    .findAll { it.containsKey("default") }
    .collectEntries { [ it.plainName, it.default ] }

  // fetch overrides in params
  def paramArgs = config.functionality.allArguments
    .findAll { params.containsKey(it.plainName) }
    .collectEntries { [ it.plainName, params[it.plainName] ] }
  
  // check multi input params
  // objects should be closures and not functions, thanks to FunctionDef
  def multiParamFormat = guessMultiParamFormat(params)

  def multiOptionFunctions = [ 
    "csv": {[it, readCsv(it)]},
    "json": {[it, readJson(it)]},
    "yaml": {[it, readYaml(it)]},
    "yaml_blob": {[null, readYamlBlob(it)]},
    "asis": {[null, it]},
    "none": {[null, [[:]]]}
  ]
  assert multiOptionFunctions.containsKey(multiParamFormat): 
    "Format of provided --param_list not recognised.\n" +
    "You can use '--param_list_format' to manually specify the format.\n" +
    "Found: '$multiParamFormat'. Expected: one of 'csv', 'json', 'yaml', 'yaml_blob', 'asis' or 'none'"

  // fetch multi param inputs
  def multiOptionFun = multiOptionFunctions.get(multiParamFormat)
  // todo: add try catch
  def multiOptionOut = multiOptionFun(params.containsKey("param_list") ? params.param_list : "")
  def paramList = multiOptionOut[1]
  def multiFile = multiOptionOut[0]

  // data checks
  assert paramList instanceof List: "--param_list should contain a list of maps"
  for (value in paramList) {
    assert value instanceof Map: "--param_list should contain a list of maps"
  }
  
  // combine parameters
  def processedParams = paramList.collect{ multiParam ->
    // combine params
    def combinedArgs = defaultArgs + multiParam + paramArgs

    // check whether required arguments exist
    config.functionality.allArguments
      .findAll { it.required }
      .forEach { par ->
        assert combinedArgs.containsKey(par.plainName): "Argument ${par.plainName} is required but does not have a value"
      }
    
    // process arguments
    def inputs = config.functionality.allArguments
      .findAll{ par -> combinedArgs.containsKey(par.plainName) }
      .collectEntries { par ->
        // split on 'multiple_sep'
        if (par.multiple) {
          parData = combinedArgs[par.plainName]
          if (parData instanceof List) {
            parData = parData.collect{it instanceof String ? it.split(par.multiple_sep) : it }
          } else if (parData instanceof String) {
            parData = parData.split(par.multiple_sep)
          } else if (parData == null) {
            parData = []
          } else {
            parData = [ parData ]
          }
        } else {
          parData = [ combinedArgs[par.plainName] ]
        }

        // flatten
        parData = parData.flatten()

        // cast types
        if (par.type == "file" && ((par.direction ?: "input") == "input")) {
          parData = parData.collect{path ->
            if (path !instanceof String) {
              path
            } else if (multiFile) {
              file(getChild(multiFile, path))
            } else {
              file(path)
            }
          }.flatten()
        } else if (par.type == "integer") {
          parData = parData.collect{it as Integer}
        } else if (par.type == "double") {
          parData = parData.collect{it as Double}
        } else if (par.type == "boolean") {
          parData = parData.collect{it as Boolean}
        }
        // simplify list to value if need be
        if (!par.multiple) {
          assert parData.size() == 1 : 
            "Error: argument ${par.plainName} has too many values.\n" +
            "  Expected amount: 1. Found: ${parData.length}"
          parData = parData[0]
        }

        // return pair
        [ par.plainName, parData ]
      }
      // remove parameters which were explicitly set to null
      .findAll{ par -> par != null }
    }
    
  
  // check processed params
  processedParams.forEach { args ->
    assert args.containsKey("id"): "Each argument set should have an 'id'. Argument set: $args"
  }
  def ppIds = processedParams.collect{it.id}
  assert ppIds.size() == ppIds.unique().size() : "All argument sets should have unique ids. Detected ids: $ppIds"

  processedParams
}

def paramsToChannel(params, config) {
  Channel.fromList(paramsToList(params, config))
}

def viashChannel(params, config) {
  paramsToChannel(params, config)
    | map{tup -> [tup.id, tup]}
}
