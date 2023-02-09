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

// param helpers //
def paramExists(name) {
  return params.containsKey(name) && params[name] != ""
}

def assertParamExists(name, description) {
  if (!paramExists(name)) {
    exit 1, "ERROR: Please provide a --${name} parameter ${description}"
  }
}

// helper functions for reading params from file //
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
      header = splitRegex.split(line, -1).collect{field ->
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
      def predata = splitRegex.split(line, -1)
      def data = predata.collect{field ->
        if (field == "") {
          return null
        }
        m = removeQuote.matcher(field)
        if (m.find()) {
          return m.replaceFirst('$1')
        } else {
          return field
        }
      }
      assert header.size() == data.size(): "Row $row should contain the same number as fields as the header"
      
      def dataMap = [header, data].transpose().collectEntries().findAll{it.value != null}
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

// helper functions for reading a viash config in groovy //

// based on how Functionality.scala is implemented
def processArgument(arg) {
  arg.multiple = arg.multiple ?: false
  arg.required = arg.required ?: false
  arg.direction = arg.direction ?: "input"
  arg.multiple_sep = arg.multiple_sep ?: ":"
  arg.plainName = arg.name.replaceAll("^-*", "")

  if (arg.type == "file") {
    arg.must_exist = arg.must_exist ?: true
    arg.create_parent = arg.create_parent ?: true
  }

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

  if (arg.type == "boolean_true") {
    arg.default = false
  }
  if (arg.type == "boolean_false") {
    arg.default = true
  }

  arg
}

// based on how Functionality.scala is implemented
def processArgumentGroup(argumentGroups, name, arguments) {
  def argNamesInGroups = argumentGroups.collectMany{it.arguments.findAll{it instanceof String}}.toSet()

  // Check if 'arguments' is in 'argumentGroups'. 
  def argumentsNotInGroup = arguments.findAll{arg -> !(argNamesInGroups.contains(arg.plainName))}

  // Check whether an argument group of 'name' exists.
  def existing = argumentGroups.find{gr -> name == gr.name}

  // if there are no arguments missing from the argument group, just return the existing group (if any)
  if (argumentsNotInGroup.isEmpty()) {
    return existing == null ? [] : [existing]
  
  // if there are missing arguments and there is an existing group, add the missing arguments to it
  } else if (existing != null) {
    def newEx = existing.clone()
    newEx.arguments.addAll(argumentsNotInGroup.findAll{it !instanceof String})
    return [newEx]

  // else create a new group
  } else {
    def newEx = [name: name, arguments: argumentsNotInGroup.findAll{it !instanceof String}]
    return [newEx]
  }
}

// based on how Functionality.scala is implemented
def processConfig(config) {
  // TODO: assert .functionality etc.
  if (config.functionality.inputs) {
    System.err.println("Warning: .functionality.inputs is deprecated. Please use .functionality.arguments instead.")
  }
  if (config.functionality.outputs) {
    System.err.println("Warning: .functionality.outputs is deprecated. Please use .functionality.arguments instead.")
  }

  // set defaults for inputs
  config.functionality.inputs = 
    (config.functionality.inputs ?: []).collect{arg ->
      arg.type = arg.type ?: "file"
      arg.direction = "input"
      processArgument(arg)
    }
  // set defaults for outputs
  config.functionality.outputs = 
    (config.functionality.outputs ?: []).collect{arg ->
      arg.type = arg.type ?: "file"
      arg.direction = "output"
      processArgument(arg)
    }
  // set defaults for arguments
  config.functionality.arguments = 
    (config.functionality.arguments ?: []).collect{arg ->
      processArgument(arg)
    }
  // set defaults for argument_group arguments
  config.functionality.argument_groups =
    (config.functionality.argument_groups ?: []).collect{grp ->
      grp.arguments = (grp.arguments ?: []).collect{arg ->
        arg instanceof String ? arg.replaceAll("^-*", "") : processArgument(arg)
      }
      grp
    }

  // create combined arguments list
  config.functionality.allArguments = 
    config.functionality.inputs +
    config.functionality.outputs +
    config.functionality.arguments +
    config.functionality.argument_groups.collectMany{ group ->
      group.arguments.findAll{ it !instanceof String }
    }
  
  // add missing argument groups (based on Functionality::allArgumentGroups())
  def argGroups = config.functionality.argument_groups
  def inputGroup = processArgumentGroup(argGroups, "Inputs", config.functionality.inputs)
  def outputGroup = processArgumentGroup(argGroups, "Outputs", config.functionality.outputs)
  def defaultGroup = processArgumentGroup(argGroups, "Arguments", config.functionality.arguments)
  def groupsFiltered = argGroups.findAll(gr -> !(["Inputs", "Outputs", "Arguments"].contains(gr.name)))
  config.functionality.allArgumentGroups = inputGroup + outputGroup + defaultGroup + groupsFiltered

  config
}

def readConfig(file) {
  def config = readYaml(file ?: "$projectDir/config.vsh.yaml")
  processConfig(config)
}

// recursively merge two maps
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

def addGlobalParams(config) {
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

  return processConfig(mergeMap(config, localConfig))
}

// helper functions for generating help // 

// based on io.viash.helpers.Format.wordWrap
def formatWordWrap(str, maxLength) {
  def words = str.split("\\s").toList()

  def word = null
  def line = ""
  def lines = []
  while(!words.isEmpty()) {
    word = words.pop()
    if (line.length() + word.length() + 1 <= maxLength) {
      line = line + " " + word
    } else {
      lines.add(line)
      line = word
    }
    if (words.isEmpty()) {
      lines.add(line)
    }
  }
  return lines
}

// based on Format.paragraphWrap
def paragraphWrap(str, maxLength) {
  def outLines = []
  str.split("\n").each{par ->
    def words = par.split("\\s").toList()

    def word = null
    def line = words.pop()
    while(!words.isEmpty()) {
      word = words.pop()
      if (line.length() + word.length() + 1 <= maxLength) {
        line = line + " " + word
      } else {
        outLines.add(line)
        line = word
      }
    }
    if (words.isEmpty()) {
      outLines.add(line)
    }
  }
  return outLines
}

def generateArgumentHelp(param) {
  // alternatives are not supported
  // def names = param.alternatives ::: List(param.name)

  def unnamedProps = [
    ["required parameter", param.required],
    ["multiple values allowed", param.multiple],
    ["output", param.direction.toLowerCase() == "output"],
    ["file must exist", param.type == "file" && param.must_exist]
  ].findAll{it[1]}.collect{it[0]}
  
  def dflt = null
  if (param.default != null) {
    if (param.default instanceof List) {
      dflt = param.default.join(param.multiple_sep ?: ", ")
    } else {
      dflt = param.default.toString()
    }
  }
  def example = null
  if (param.example != null) {
    if (param.example instanceof List) {
      example = param.example.join(param.multiple_sep ?: ", ")
    } else {
      example = param.example.toString()
    }
  }
  def min = param.min?.toString()
  def max = param.max?.toString()

  def escapeChoice = { choice ->
    def s1 = choice.replaceAll("\\n", "\\\\n")
    def s2 = s1.replaceAll("\"", """\\\"""")
    s2.contains(",") || s2 != choice ? "\"" + s2 + "\"" : s2
  }
  def choices = param.choices == null ? 
    null : 
    "[ " + param.choices.collect{escapeChoice(it.toString())}.join(", ") + " ]"

  def namedPropsStr = [
    ["type", ([param.type] + unnamedProps).join(", ")],
    ["default", dflt],
    ["example", example],
    ["choices", choices],
    ["min", min],
    ["max", max]
  ]
    .findAll{it[1]}
    .collect{"\n        " + it[0] + ": " + it[1].replaceAll("\n", "\\n")}
    .join("")
  
  def descStr = param.description == null ?
    "" :
    paragraphWrap("\n" + param.description.trim(), 80 - 8).join("\n        ")
  
  "\n    --" + param.plainName +
    namedPropsStr +
    descStr
}

// Based on Helper.generateHelp() in Helper.scala
def generateHelp(config) {
  def fun = config.functionality

  // PART 1: NAME AND VERSION
  def nameStr = fun.name + 
    (fun.version == null ? "" : " " + fun.version)

  // PART 2: DESCRIPTION
  def descrStr = fun.description == null ? 
    "" :
    "\n\n" + paragraphWrap(fun.description.trim(), 80).join("\n")

  // PART 3: Usage
  def usageStr = fun.usage == null ? 
    "" :
    "\n\nUsage:\n" + fun.usage.trim()

  // PART 4: Options
  def argGroupStrs = fun.allArgumentGroups.collect{argGroup ->
    def name = argGroup.name
    def descriptionStr = argGroup.description == null ?
      "" :
      "\n    " + paragraphWrap(argGroup.description.trim(), 80-4).join("\n    ") + "\n"
    def arguments = argGroup.arguments.collect{arg -> 
      arg instanceof String ? fun.allArguments.find{it.plainName == arg} : arg
    }.findAll{it != null}
    def argumentStrs = arguments.collect{param -> generateArgumentHelp(param)}
    
    "\n\n$name:" +
      descriptionStr +
      argumentStrs.join("\n")
  }

  // FINAL: combine
  def out = nameStr + 
    descrStr +
    usageStr + 
    argGroupStrs.join("")

  return out
}

def helpMessage(config) {
  if (paramExists("help")) {
    def mergedConfig = addGlobalParams(config)
    def helpStr = generateHelp(mergedConfig)
    println(helpStr)
    exit 0
  }
}

def guessMultiParamFormat(params) {
  if (!params.containsKey("param_list") || params.param_list == null) {
    "none"
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
    def combinedArgs = defaultArgs + paramArgs + multiParam

    if (workflow.stubRun) {
      // if stub run, explicitly add an id if missing
      combinedArgs = [id: "stub"] + combinedArgs
    } else {
      // else check whether required arguments exist
      config.functionality.allArguments
        .findAll { it.required }
        .forEach { par ->
          assert combinedArgs.containsKey(par.plainName): "Argument ${par.plainName} is required but does not have a value"
        }
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
        } else if (par.type == "boolean" || par.type == "boolean_true" || par.type == "boolean_false") {
          parData = parData.collect{it as Boolean}
        }
        // simplify list to value if need be
        if (!par.multiple) {
          assert parData.size() == 1 : 
            "Error: argument ${par.plainName} has too many values.\n" +
            "  Expected amount: 1. Found: ${parData.size()}"
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

def _splitParams(Map paramList, List<Map> multiArgumentsSettings){
  /*
   *   Split parameters for arguments that accept multiple values using their seperator
   *
   *   paramList: a Map containing parameters to split
   *   multiArgumentsSettings: a List of the viash configuration argument entries 
   *                           that have the property 'multiple: true'
   */
  paramList.collectEntries { parName, parValue ->
    parameterSettings = multiArgumentsSettings.find({it.plainName == parName})
    if (parameterSettings) { // Check if parameter can accept multiple values
      if (parValue instanceof List) {
          parValue = parValue.collect{it instanceof String ? it.split(parameterSettings.multiple_sep) : it }
      } else if (parValue instanceof String) {
          parValue = parValue.split(parameterSettings.multiple_sep)
      } else if (parValue == null) {
          parValue = []
      } else {
          parValue = [ parValue ]
      }
      parValue = parValue.flatten()
    }
    [parName, parValue]
  }
}

def _checkUniqueIds(List<Map> multiParam) {
  /*
   *  Check if the ids are unique acorss parameter sets
   */
  def ppIds = multiParam.collect{it[0]}
  assert ppIds.size() == ppIds.unique().size() : "All argument sets should have unique ids. Detected ids: $ppIds"
}

def _resolvePathsRelativeTo(Map paramList, List<Map<String, String>> inputFileSetttings, String relativeTo) {
  /*
   *   Resolve the file paths in the parameters relative to given path
   *
   *   paramList: a Map containing parameters to process.
   *              This function assumes that files are still of type String.
   *   inputFileSetttings: a Map of the viash configuration argument entries 
   *                       that are considered input files (i.e. with 'direction: input'
   *                       and 'type: file')
   *   relativeTo: path of a file to resolve the parameters values to.
   */
  paramList.collectEntries { parName, parValue ->
    isInputFile = inputFileSetttings.find({it.plainName == parName})
    if (isInputFile) {
      if (parValue instanceof List) {
        parValue = parValue.collect({path -> 
          path !instanceof String ? path : file(getChild(multiFile, path))
        })
      } else {
        parValue = parValue !instanceof String ? path : file(getChild(multiFile, path))
      }
    }
  [parName, parValue]
  }
}

def _parseMultiArguments(Map params, List<Map> multiArguments){
  /*
   *   Parse multiple parameter sets passed using param_list into a list of parameter sets.
   *
   *   params: input parameters from nextflow.
   *   multiArgumentsSettings: a List of the viash configuration argument entries 
   *                           that have the property 'multiple: true'
   */

  // first try to guess the format (if not set in params)
  def multiParamFormat = guessMultiParamFormat(params)

  // get the correct parser function for the detected params_list format
  def multiOptionsParsers = [ 
    "csv": {[it, readCsv(it)]},
    "json": {[it, readJson(it)]},
    "yaml": {[it, readYaml(it)]},
    "yaml_blob": {[null, readYamlBlob(it)]},
    "asis": {[null, it]},
    "none": {[null, [[:]]]}
  ]
  assert multiOptionsParsers.containsKey(multiParamFormat):
    "Format of provided --param_list not recognised.\n" +
    "You can use '--param_list_format' to manually specify the format.\n" +
    "Found: '$multiParamFormat'. Expected: one of 'csv', 'json', "+
    "'yaml', 'yaml_blob', 'asis' or 'none'"
  def multiOptionParser = multiOptionsParsers.get(multiParamFormat)

  // fetch multi param inputs
  def multiOptionOut = multiOptionParser(params.containsKey("param_list") ? params.param_list : "")
  // multiFile is null if the value passed to param_list was not a file (e.g a blob)
  // If the value was indeed a file, multiFile contains the location that file (used later).
  def multiFile = multiOptionOut[0]
  def multiParam = multiOptionOut[1] // these are the actual parameters from reading the blob/file

  // Split parameters with 'multiple: true'
  multiParam = multiParam.collect({_splitParams(it, multiArguments)})
  
  // The paths of input files inside a param_list file may have been specified relatively to the
  // location of the param_list file. These paths must be made absolute.
  if (multiFile){
    multiParam = inputfileArguments.collect({
      _resolvePathsRelativeTo(paramList, inputfileArguments, multiFile)
    })
  }
  
  // data checks
  assert multiParam instanceof List: "--param_list should contain a list of maps"
  for (value in multiParam) {
    assert value instanceof Map: "--param_list should contain a list of maps"
  }

  return multiParam
}

def channelFromParams(Map params, Map config) {
  /*
   *   Parse nextflow parameters based on settings defined in a viash config.
   *   The config file should have been preparsed using the readConfig() function,
   *   which can also be found in this module. 
   *
   *   This function performs:
   *      - A filtering of the params which can be found in the config file.
   *      - Process the params_list argument which allows a user to to initialise 
   *        a Vsdl3 channel with multiple parameter sets. Possible formats are 
   *        csv, json, yaml, or simply a yaml_blob. A csv should have column names 
   *        which correspond to the different arguments of this pipeline. A json or a yaml
   *        file should be a list of maps, each of which has keys corresponding to the
   *        arguments of the pipeline. A yaml blob can also be passed directly as a parameter.
   *        When passing a csv, json or yaml, relative path names are relativized to the
   *        location of the parameter file.
   *      - Combine the parameter sets into a vdsl3 Channel.
   *
   *   params: input parameters from nextflow.
   *   config: a Map of the viash configuration.
   * 
   */
  
  /* Get different parameter types */
  /*********************************/
  def configArguments = config.functionality.allArguments // List[Map]
  def plainNameArguments = configArguments.findAll{it.containsKey("plainName")}
  def inputfileArguments = plainNameArguments.findAll({it.type == "file" && ((it.direction ?: "input") == "input")})
  def multiArguments = plainNameArguments.findAll({it.multiple})
  def paramArgs = params.findAll({ !( it.key in ["root-dir", "rootDir", "param_list"]) })

  /* process params_list arguments */
  /*********************************/
  multiParam = _parseMultiArguments(params, multiArguments)

  /* combine arguments into channel */
  /**********************************/

  def processedParams = multiParam.collect{ paramList ->
    // Add regular parameters together with parameters passed with 'param_list'
    def combinedArgs = paramArgs + paramList

    if (workflow.stubRun) {
      // if stub run, explicitly add an id if missing
      combinedArgs = [id: "stub"] + combinedArgs
    }
    // Move id to position 0
    assert (combinedArgs.id != null): "All argument sets should have and id"
    combinedArgs = [combinedArgs.id, combinedArgs.findAll{it.key != "id"}]

    // Remove parameters which are null, if the default is also null
    combinedArgs[1] = combinedArgs[1].collectEntries{paramName, paramValue ->
      parameterSettings = plainNameArguments.find({it.plainName == paramName})
      if ( paramValue != null || parameterSettings.get("default", null) != null ) {
        [paramName, paramValue]
      }
    }
    combinedArgs
  }

  // Check if ids (first element of each list) is unique
  _checkUniqueIds(processedParams)
  return Channel.fromList(processedParams)
}


def preprocessInputs(Map config) {
  workflow preprocworkflow {
    take: 
    input_ch

    main:
    
    assert config instanceof Map : 
      "Error in preprocessInputs: config must be a map. " +
      "Expected class: Map. Found: config.getClass() is ${config.getClass()}"

    // Get different parameter types (used throughout this function)
    def configArguments = config.functionality.allArguments
    def defaultArgs = configArguments
      .findAll { it.containsKey("default") }
      .collectEntries { [ it.plainName, it.default ] }
    def multiArguments = configArguments
      .findAll({it.containsKey("plainName") && it.multiple})

    output_ch = input_ch
      | view {"Start of preprocess: $it"}
      // split on multi_sep

      // add defaults
      | map { tuple -> 
        tuple = tuple.clone()

        id = tuple[0]
        arguments = tuple[1]
        passthrough = tuple.drop(2)        

        // Take care that manual overrides are 
        // not overridden by the default value!
        arguments = defaultArgs + arguments

        // Split parameters with 'multiple: true'
        arguments = _splitParams(arguments, multiArguments)

        // Cast the input to the correct type according to viash config
        arguments = arguments.collectEntries({ parName, parValue ->
          paramSettings = configArguments.find({it.plainName == parName})
          // dont parse parameters like publish_dir ( in which case paramSettings = null)
          parType = paramSettings ? paramSettings.get("type", null) : null
          if (! (parValue instanceof Collection)) {
            parValue = [parValue]
          }
          if (parType == "integer") {
            parValue = parValue.collect{it as Integer}
          } else if (parType == "double") {
            parValue = parValue.collect{it as Double}
          } else if (parType == "boolean" || 
                     parType == "boolean_true" || 
                     parType == "boolean_false") {
            parValue = parValue.collect{it as Boolean}
          }

          // simplify list to value if need be
          if (paramSettings && !paramSettings.multiple) {
            assert parValue.size() == 1 : 
              "Error: argument ${parName} has too many values.\n" +
              "  Expected amount: 1. Found: ${parValue.size()}"
            parValue = parValue[0]
          }
          [parName, parValue]
        })

        // Check if any unexpected arguments were passed
        def knownParams = configArguments.collect({it.plainName}) + ["publishDir", "publish_dir"]
        arguments.each({parName, parValue ->
          assert parName in knownParams: "Unknown parameter. Parameter $parName should be in $knownParams"
        })

        // Return with passthrough
        [id, arguments] + passthrough
      }
    emit:
    output_ch
  }

  return preprocworkflow
}
