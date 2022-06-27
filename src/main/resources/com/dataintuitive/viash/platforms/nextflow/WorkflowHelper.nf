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

def processConfig(config) {
  // assert .functionality etc.
  config.functionality.arguments = 
    config.functionality.arguments.collect{arg ->
      // fill in defaults
      arg.multiple = arg.multiple ?: false
      arg.required = arg.required ?: false
      arg.direction = arg.direction ?: "input"
      arg.multiple_sep = arg.multiple_sep ?: ":"
      arg.plainName = arg.name.replaceAll("^--", "")
      arg
    }
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

    localConfig = [
      "functionality" : [
        "arguments": [
          [
            'name': '--publishDir',
            'plainName' : 'publishDir',
            'required': true,
            'type': 'string',
            'description': 'Path to an output directory.',
            'example': 'output/',
            'multiple': false
          ]
        ],
        "arguments_groups": [
          [
            "name": "Output",
            "order": 100,
            "arguments" : [ "publishDir" ]
          ]
        ]
      ]
    ]

    def template = '''\
    |${functionality.name}
    |
    |${functionality.description}
    |
    |Single-input parameters:
    |Parameters (Single input mode):
    |<% for (group in (functionality.arguments_groups.size() > 1 ) ? functionality.arguments_groups : [ [ name: "All parameters", order: 0, arguments: functionality.arguments.collect{ it.plainName } ] ] ) { %>
    |<%= group.name ? ("  " + group.name + "          ").take(16) : "" %><%= group.description ? group.description : "" %>
    |<% for (argument in functionality.arguments) { %><% if (group.arguments.contains(argument.plainName)) {  %><%= ("  " + argument.name + "          ").take(16) %><%= argument.description %> <%= argument.required  ? '(required)' : '(optional, default = ' + argument.default + ')' %>
    |<% } } } %>
    |
    |Multi-input parameters:
    |
    |  --params_csv, --paramsCsv              specify parameters with a comma separated tabular file.
    |  --params_json, --paramsJson            specify parameters with a Json file.
    |  --params_json_blob, --paramsJsonBlob   specify parameters with a Json blob.
    |  --params_yaml, --paramsYaml            specify parameters with a Yaml file.
    |  --params_yaml_blob, --paramsYamlBlob   specify parameters with a Yaml blob.
    |
    |  params.params_list, params.paramsList  specify parameters with a Groovy list inside the Nextflow config.
    '''.stripMargin()

    def engine = new groovy.text.SimpleTemplateEngine()
    def mergedConfig = mergeMap(config, localConfig)
    def help = engine.createTemplate(template).make(mergedConfig)
    println(help)
    exit 0

  }
}

def paramsToList(params, config) {
  // fetch default params from functionality
  def defaultArgs = config.functionality.arguments
    .findAll { it.containsKey("default") }
    .collectEntries { [ it.plainName, it.default ] }

  // fetch overrides in params
  def paramArgs = config.functionality.arguments
    .findAll { params.containsKey(it.plainName) }
    .collectEntries { [ it.plainName, params[it.plainName] ] }
  
  // check multi input params
  // objects should be closures and not functions, thanks to FunctionDef
  def multiOptionFunctions = [ 
    "params_csv": {[it, readCsv(it)]},
    "paramsCsv": {[it, readCsv(it)]},
    "params_json": {[it, readJson(it)]},
    "paramsJson": {[it, readJson(it)]},
    "params_json_blob": {[null, readJsonBlob(it)]},
    "paramsJsonBlob": {[null, readJsonBlob(it)]},
    "params_yaml": {[it, readYaml(it)]},
    "paramsYaml": {[it, readYaml(it)]},
    "params_yaml_blob": {[null, readYamlBlob(it)]},
    "paramsYamlBlob": {[null, readYamlBlob(it)]},
    "params_list": {[null, it]},
    "paramsList": {[null, it]}
  ]
  def multiParamsCheck = multiOptionFunctions.findAll{k, v -> params.containsKey(k)}.collect()
  assert multiParamsCheck.size() <= 1: "At most one of '${multiOptionFunctions.keySet().join("', '")}' should be specified"

  // fetch multi param inputs
  def multiParams = [[:]]
  def multiFile = null
  if (multiParamsCheck.size() == 1) {
    def readerId = multiParamsCheck[0].key
    def multiReader = multiParamsCheck[0].value
    out = multiReader(params[readerId])
    multiFile = out[0]
    multiParams = out[1]
  }

  // data checks
  assert multiParams instanceof List: "--$readerId should contain a list of maps"
  for (value in multiParams) {
    assert value instanceof Map: "--$readerId should contain a list of maps"
  }
  
  // combine parameters
  def processedParams = multiParams.collect{ multiParam ->
    // combine params
    def combinedArgs = defaultArgs + multiParam + paramArgs

    // check whether required arguments exist
    config.functionality.arguments
      .findAll { it.required }
      .forEach { par ->
        assert combinedArgs.containsKey(par.plainName): "Argument ${par.plainName} is required but does not have a value"
      }
    
    // process arguments
    def inputs = config.functionality.arguments
      .collectEntries { par ->
        // split on 'multiple_sep'
        if (par.multiple) {
          parData = combinedArgs[par.plainName]
          if (parData instanceof List) {
            parData = parData.collect{it instanceof String ? it.split(par.multiple_sep) : it }
          } else if (parData instanceof String) {
            parData = parData.split(par.multiple_sep)
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

    }
  
  // check processed params
  processedParams.forEach { args ->
    assert args.containsKey("id"): "Each argument set should have an 'id'. Argument set: $args"
  }
  def ppIds = processedParams{it.id}
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
