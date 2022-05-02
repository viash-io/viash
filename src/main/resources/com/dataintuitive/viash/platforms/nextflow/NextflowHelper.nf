import nextflow.script.IncludeDef
import nextflow.script.ScriptBinding
import nextflow.script.ScriptMeta
import nextflow.script.ScriptParser

resourcesDir = ScriptMeta.current().getScriptPath().getParent()

def assertMapKeys(map, expectedKeys, requiredKeys, mapName) {
  assert map instanceof Map : "Expected argument '$mapName' to be a Map. Found: class ${map.getClass()}"
  map.forEach { key, val -> 
    assert key in expectedKeys : "Unexpected key '$key' in ${mapName ? mapName + " " : ""}map"
  }
  requiredKeys.forEach { requiredKey -> 
    assert map.containsKey(requiredKey) : "Missing required key '$key' in ${mapName ? mapName + " " : ""}map"
  }
}

// TODO: unit test processDirectives
def processDirectives(Map drctv) {
  // remove null values
  drctv = drctv.findAll{k, v -> v}

  /* DIRECTIVE accelerator
    accepted examples:
    - [ limit: 4, type: "nvidia-tesla-k80" ]
  */
  if (drctv.containsKey("accelerator")) {
    assertMapKeys(drctv["accelerator"], ["type", "limit", "request", "runtime"], [], "accelerator")
  }

  /* DIRECTIVE afterScript
    accepted examples:
    - "source /cluster/bin/cleanup"
  */
  if (drctv.containsKey("afterScript")) {
    assert drctv["afterScript"] instanceof String
  }

  /* DIRECTIVE beforeScript
    accepted examples:
    - "source /cluster/bin/setup"
  */
  if (drctv.containsKey("beforeScript")) {
    assert drctv["beforeScript"] instanceof String
  }

  /* DIRECTIVE cache
    accepted examples:
    - true
    - false
    - "deep"
    - "lenient"
  */
  if (drctv.containsKey("cache")) {
    assert drctv["cache"] instanceof String || drctv["cache"] instanceof Boolean
    if (drctv["cache"] instanceof String) {
      assert drctv["cache"] in ["deep", "lenient"] : "Unexpected value for cache"
    }
  }

  /* DIRECTIVE conda
    accepted examples:
    - "bwa=0.7.15"
    - "bwa=0.7.15 fastqc=0.11.5"
    - ["bwa=0.7.15", "fastqc=0.11.5"]
  */
  if (drctv.containsKey("conda")) {
    if (drctv["conda"] instanceof List) {
      drctv["conda"] = drctv["conda"].join(" ")
    }
    assert drctv["conda"] instanceof String
  }

  /* DIRECTIVE container
    accepted examples:
    - "foo/bar:tag"
    - [ registry: "reg", image: "im", tag: "ta" ]
      is transformed to "reg/im:ta"
    - [ image: "im" ] 
      is transformed to "im:latest"
  */
  if (drctv.containsKey("container")) {
    assert drctv["container"] instanceof Map || drctv["container"] instanceof String
    if (drctv["container"] instanceof Map) {
      def m = drctv["container"]
      assertMapKeys(m, [ "registry", "image", "tag" ], ["image"], "container")
      def part1 = m.registry ? m.registry + "/" : ""
      def part2 = m.image
      def part3 = m.tag ? ":" + m.tag : ":latest"
      drctv["container"] = part1 + part2 + part3
    }
  }

  /* DIRECTIVE containerOptions
    accepted examples:
    - "--foo bar"
    - ["--foo bar", "-f b"]
  */
  if (drctv.containsKey("containerOptions")) {
    if (drctv["containerOptions"] instanceof List) {
      drctv["containerOptions"] = drctv["containerOptions"].join(" ")
    }
    assert drctv["containerOptions"] instanceof String
  }

  /* DIRECTIVE cpus
    accepted examples:
    - 1
    - 10
  */
  if (drctv.containsKey("cpus")) {
    assert drctv["cpus"] instanceof Integer
  }

  /* DIRECTIVE disk
    accepted examples:
    - "1 GB"
    - "2TB"
    - "3.2KB"
    - "10.B"
  */
  if (drctv.containsKey("disk")) {
    assert drctv["disk"] instanceof String
    // assert drctv["disk"].matches("[0-9]+(\\.[0-9]*)? *[KMGTPEZY]?B")
    // ^ does not allow closures
  }

  /* DIRECTIVE echo
    accepted examples:
    - true
    - false
  */
  if (drctv.containsKey("echo")) {
    assert drctv["echo"] instanceof Boolean
  }

  /* DIRECTIVE errorStrategy
    accepted examples:
    - "terminate"
    - "finish"
  */
  if (drctv.containsKey("errorStrategy")) {
    assert drctv["errorStrategy"] instanceof String
    assert drctv["errorStrategy"] in ["terminate", "finish", "ignore", "retry"] : "Unexpected value for errorStrategy"
  }

  /* DIRECTIVE executor
    accepted examples:
    - "local"
    - "sge"
  */
  if (drctv.containsKey("executor")) {
    assert drctv["executor"] instanceof String
    assert drctv["executor"] in ["local", "sge", "uge", "lsf", "slurm", "pbs", "pbspro", "moab", "condor", "nqsii", "ignite", "k8s", "awsbatch", "google-pipelines"] : "Unexpected value for executor"
  }

  /* DIRECTIVE machineType
    accepted examples:
    - "n1-highmem-8"
  */
  if (drctv.containsKey("machineType")) {
    assert drctv["machineType"] instanceof String
  }

  /* DIRECTIVE maxErrors
    accepted examples:
    - 1
    - 3
  */
  if (drctv.containsKey("maxErrors")) {
    assert drctv["maxErrors"] instanceof Integer
  }

  /* DIRECTIVE maxForks
    accepted examples:
    - 1
    - 3
  */
  if (drctv.containsKey("maxForks")) {
    assert drctv["maxForks"] instanceof Integer
  }

  /* DIRECTIVE maxRetries
    accepted examples:
    - 1
    - 3
  */
  if (drctv.containsKey("maxRetries")) {
    assert drctv["maxRetries"] instanceof Integer
  }

  /* DIRECTIVE memory
    accepted examples:
    - "1 GB"
    - "2TB"
    - "3.2KB"
    - "10.B"
  */
  if (drctv.containsKey("memory")) {
    assert drctv["memory"] instanceof String
    // assert drctv["memory"].matches("[0-9]+(\\.[0-9]*)? *[KMGTPEZY]?B")
    // ^ does not allow closures
  }

  /* DIRECTIVE module
    accepted examples:
    - "ncbi-blast/2.2.27"
    - "ncbi-blast/2.2.27:t_coffee/10.0"
    - ["ncbi-blast/2.2.27", "t_coffee/10.0"]
  */
  if (drctv.containsKey("module")) {
    if (drctv["module"] instanceof List) {
      drctv["module"] = drctv["module"].join(":")
    }
    assert drctv["module"] instanceof String
  }

  /* DIRECTIVE penv
    accepted examples:
    - "smp"
  */
  if (drctv.containsKey("penv")) {
    assert drctv["penv"] instanceof String
  }

  /* DIRECTIVE pod
    accepted examples:
    - [ label: "key", value: "val" ]
    - [ annotation: "key", value: "val" ]
    - [ env: "key", value: "val" ]
    - [ [label: "l", value: "v"], [env: "e", value: "v"]]
  */
  if (drctv.containsKey("pod")) {
    if (drctv["pod"] instanceof Map) {
      drctv["pod"] = [ drctv["pod"] ]
    }
    assert drctv["pod"] instanceof List
    drctv["pod"].forEach { pod ->
      assert pod instanceof Map
      // TODO: should more checks be added?
      // See https://www.nextflow.io/docs/latest/process.html?highlight=directives#pod
      // e.g. does it contain 'label' and 'value', or 'annotation' and 'value', or ...?
    }
  }

  /* DIRECTIVE publishDir
    accepted examples:
    - []
    - [ [ path: "foo", enabled: true ], [ path: "bar", enabled: false ] ]
    - "/path/to/dir" 
      is transformed to [[ path: "/path/to/dir" ]]
    - [ path: "/path/to/dir", mode: "cache" ]
      is transformed to [[ path: "/path/to/dir", mode: "cache" ]]
  */
  // TODO: should we also look at params["publishDir"]?
  if (drctv.containsKey("publishDir")) {
    def pblsh = drctv["publishDir"]
    
    // check different options
    assert pblsh instanceof List || pblsh instanceof Map || pblsh instanceof String
    
    // turn into list if not already so
    // for some reason, 'if (!pblsh instanceof List) pblsh = [ pblsh ]' doesn't work.
    pblsh = pblsh instanceof List ? pblsh : [ pblsh ]

    // check elements of publishDir
    pblsh = pblsh.collect{ elem ->
      // turn into map if not already so
      elem = elem instanceof String ? [ path: elem ] : elem

      // check types and keys
      assert elem instanceof Map : "Expected publish argument '$elem' to be a String or a Map. Found: class ${elem.getClass()}"
      assertMapKeys(elem, [ "path", "mode", "overwrite", "pattern", "saveAs", "enabled" ], ["path"], "publishDir")

      // check elements in map
      assert elem.containsKey("path")
      assert elem["path"] instanceof String
      if (elem.containsKey("mode")) {
        assert elem["mode"] instanceof String
        assert elem["mode"] in [ "symlink", "rellink", "link", "copy", "copyNoFollow", "move" ]
      }
      if (elem.containsKey("overwrite")) {
        assert elem["overwrite"] instanceof Boolean
      }
      if (elem.containsKey("pattern")) {
        assert elem["pattern"] instanceof String
      }
      if (elem.containsKey("saveAs")) {
        assert elem["saveAs"] instanceof String //: "saveAs as a Closure is currently not supported. Surround your closure with single quotes to get the desired effect. Example: '\{ foo \}'"
      }
      if (elem.containsKey("enabled")) {
        assert elem["enabled"] instanceof Boolean
      }

      // return final result
      elem
    }
    // store final directive
    drctv["publishDir"] = pblsh
  }

  /* DIRECTIVE queue
    accepted examples:
    - "long"
    - "short,long"
    - ["short", "long"]
  */
  if (drctv.containsKey("queue")) {
    if (drctv["queue"] instanceof List) {
      drctv["queue"] = drctv["queue"].join(",")
    }
    assert drctv["queue"] instanceof String
  }

  /* DIRECTIVE label
    accepted examples:
    - "big_mem"
    - "big_cpu"
    - ["big_mem", "big_cpu"]
  */
  if (drctv.containsKey("label")) {
    if (drctv["label"] instanceof String) {
      drctv["label"] = [ drctv["label"] ]
    }
    assert drctv["label"] instanceof List
    drctv["label"].forEach { label ->
      assert label instanceof String
      // assert label.matches("[a-zA-Z0-9]([a-zA-Z0-9_]*[a-zA-Z0-9])?")
      // ^ does not allow closures
    }
  }

  /* DIRECTIVE scratch
    accepted examples:
    - true
    - "/path/to/scratch"
    - '$MY_PATH_TO_SCRATCH'
    - "ram-disk"
  */
  if (drctv.containsKey("scratch")) {
    assert drctv["scratch"] == true || drctv["scratch"] instanceof String
  }

  /* DIRECTIVE storeDir
    accepted examples:
    - "/path/to/storeDir"
  */
  if (drctv.containsKey("storeDir")) {
    assert drctv["storeDir"] instanceof String
  }

  /* DIRECTIVE stageInMode
    accepted examples:
    - "copy"
    - "link"
  */
  if (drctv.containsKey("stageInMode")) {
    assert drctv["stageInMode"] instanceof String
    assert drctv["stageInMode"] in ["copy", "link", "symlink", "rellink"]
  }

  /* DIRECTIVE stageOutMode
    accepted examples:
    - "copy"
    - "link"
  */
  if (drctv.containsKey("stageOutMode")) {
    assert drctv["stageOutMode"] instanceof String
    assert drctv["stageOutMode"] in ["copy", "move", "rsync"]
  }

  /* DIRECTIVE tag
    accepted examples:
    - "foo"
    - '$id'
  */
  if (drctv.containsKey("tag")) {
    assert drctv["tag"] instanceof String
  }

  /* DIRECTIVE time
    accepted examples:
    - "1h"
    - "2days"
    - "1day 6hours 3minutes 30seconds"
  */
  if (drctv.containsKey("time")) {
    assert drctv["time"] instanceof String
    // todo: validation regex?
  }

  return drctv
}

def processProcessArgs(Map args) {
  // override defaults with args
  def processArgs = thisDefaultProcessArgs + args

  // check whether 'key' exists
  assert processArgs.containsKey("key")
  assert processArgs["key"] instanceof String
  assert processArgs["key"] ==~ /^[a-zA-Z_][a-zA-Z0-9_]*$/

  // check whether directives exists and apply defaults
  if (!processArgs.containsKey("directives")) {
    processArgs["directives"] = [:]
  }
  assert processArgs["directives"] instanceof Map
  processArgs["directives"] = processDirectives(thisDefaultDirectives + processArgs["directives"])

  for (nam in [ "map", "mapId", "mapData", "mapPassthrough" ]) {
    if (processArgs.containsKey(nam) && processArgs[nam]) {
      assert processArgs[nam] instanceof Closure : "Expected process argument '$nam' to be null or a Closure. Found: class ${processArgs[nam].getClass()}"
    }
  }

  // return output
  return processArgs
}

def processFactory(Map processArgs) {
  def tripQuo = "\"\"\""
  def procKey = processArgs["key"] + "_process"

  def meta = ScriptMeta.current()
  
  assert ! (procKey in meta.getProcessNames()) : 
    "Error in module '${processArgs['key']}': process key '$procKey' is already used.\n" +
    "  Make sure to specify a new key when running a Viash workflow multiple times.\n" +
    "  Example: myModule.run(key: 'foo') | myModule.run(key: 'bar')"

  // subset directives and convert to list of tuples
  def drctv = processArgs.directives

  // TODO: unit test the two commands below
  // convert publish array into tags
  def valueToStr = { val ->
    // ignore closures
    if (val instanceof String) {
      if (!val.matches('^[{].*[}]$')) {
        '"' + val + '"'
      } else {
        val
      }
    } else if (val instanceof List) {
      "[" + val.collect{valueToStr(it)}.join(", ") + "]"
    } else if (val instanceof Map) {
      "[" + val.collect{k, v -> k + ": " + valueToStr(v)}.join(", ") + "]"
    } else {
      val.inspect()
    }
  }
  // multiple entries allowed: label, publishdir
  def drctvStrs = drctv.collect { key, value ->
    if (key in ["label", "publishDir"]) {
      value.collect{ val ->
        if (val instanceof Map) {
          "\n  $key " + val.collect{ k, v -> k + ": " + valueToStr(v) }.join(", ")
        } else {
          "\n  $key " + valueToStr(val)
        }
      }.join()
    } else if (value instanceof Map) {
      "\n  $key " + value.collect{ k, v -> k + ": " + valueToStr(v) }.join(", ")
    } else {
      "\n  $key " + valueToStr(value)
    }
  }.join()

  def inputPaths = thisFunctionality.arguments
    .findAll { it.type == "file" && it.direction == "input" }
    .collect { ', path(viash_par_' + it.name + ')' }
    .join()

  def outputPaths = thisFunctionality.arguments
    .findAll { it.type == "file" && it.direction == "output" }
    .collect { par ->
      // insert dummy into every output (see nextflow-io/nextflow#2678)
      if (!par.multiple) {
        ', path{[".exitcode", args.' + par.name + ']}'
      } else {
        ', path{[".exitcode"] + args.' + par.name + '}'
      }
    }
    // .collect { par -> ', path("${args.' + par.name + '}"' + (par.required ? "" : ", optional: true") + ')' }
    .join()

  // construct inputFileExports
  def inputFileExports = thisFunctionality.arguments
    .findAll { it.type == "file" && it.direction.toLowerCase() == "input" }
    .collect { par ->
      if (!par.required && !par.multiple) {
        "\n\${viash_par_${par.name}.empty ? \"\" : \"export VIASH_PAR_${par.name.toUpperCase()}=\\\"\" + viash_par_${par.name}[0] + \"\\\"\"}"
      } else {
        "\nexport VIASH_PAR_${par.name.toUpperCase()}=\"\${viash_par_${par.name}.join(\":\")}\""
      }
    }
  
  def tmpDir = "/tmp" // check if component is docker based

  // construct stub
  def stub = thisFunctionality.arguments
    .findAll { it.type == "file" && it.direction == "output" }
    .collect { par -> 
      'touch "${viash_par_' + par.name + '.join(\'" "\')}"'
    }
    .join("\n")

  // escape script
  def escapedScript = thisScript.replace('\\', '\\\\').replace('$', '\\$').replace('"""', '\\"\\"\\"')

  // generate process string
  def procStr = 
  """nextflow.enable.dsl=2
  |
  |process $procKey {$drctvStrs
  |input:
  |  tuple val(id)$inputPaths, val(args), val(passthrough), path(resourcesDir)
  |output:
  |  tuple val("\$id"), val(passthrough)$outputPaths, optional: true
  |stub:
  |$tripQuo
  |$stub
  |$tripQuo
  |script:
  |def escapeText = { s -> s.toString().replaceAll('([`"])', '\\\\\\\\\$1') }
  |def parInject = args
  |  .findAll{key, value -> value}
  |  .collect{key, value -> "export VIASH_PAR_\${key.toUpperCase()}=\\\"\${escapeText(value)}\\\""}
  |  .join("\\n")
  |$tripQuo
  |# meta exports
  |export VIASH_META_RESOURCES_DIR="\$resourcesDir"
  |export VIASH_META_TEMP_DIR="${tmpDir}"
  |export VIASH_META_FUNCTIONALITY_NAME="${thisFunctionality.name}"
  |
  |# meta synonyms
  |export VIASH_RESOURCES_DIR="\\\$VIASH_META_RESOURCES_DIR"
  |export VIASH_TEMP="\\\$VIASH_META_TEMP_DIR"
  |export TEMP_DIR="\\\$VIASH_META_TEMP_DIR"
  |
  |# argument exports${inputFileExports.join()}
  |\$parInject
  |
  |# process script
  |${escapedScript}
  |$tripQuo
  |}
  |""".stripMargin()

  // TODO: print on debug

  // create runtime process
  def ownerParams = new ScriptBinding.ParamsMap()
  def binding = new ScriptBinding().setParams(ownerParams)
  def module = new IncludeDef.Module(name: procKey)
  def moduleScript = new ScriptParser(session)
    .setModule(true)
    .setBinding(binding)
    .runScript(procStr)
    .getScript()

  // register module in meta
  meta.addModule(moduleScript, module.name, module.alias)

  // retrieve process from meta
  return meta.getProcess(procKey)
}

def debug(processArgs, debugKey) {
  if (processArgs.debug) {
    view { "process '${processArgs.key}' $debugKey tuple: $it"  }
  } else {
    map { it }
  }
}

def workflowFactory(Map args) {
  def processArgs = processProcessArgs(args)
  def workflowKey = processArgs["key"]
  def meta = ScriptMeta.current()

  assert ! (workflowKey in meta.getAllNames()) : 
    "Error in module '${workflowKey}': workflow key '$workflowKey' is already used.\n" +
    "  Make sure to specify a new key when running a Viash module multiple times.\n" +
    "  Example: myModule.run(key: 'foo') | myModule.run(key: 'bar')\n" +
    "  Expected: ! '$workflowKey' in ScriptMeta.current().getAllNames()"

  // write process to temporary nf file and parse it in memory
  def processObj = processFactory(processArgs)
  
  workflow workflowInstance {
    take:
    input_

    main:
    output_ = input_
      | debug(processArgs, "input")
      | map { tuple ->
        if (processArgs.map) {
          tuple = processArgs.map(tuple)
        }
        if (processArgs.mapId) {
          tuple[0] = processArgs.mapId(tuple[0])
        }
        if (processArgs.mapData) {
          tuple[1] = processArgs.mapData(tuple[1])
        }
        if (processArgs.mapPassthrough) {
          tuple = tuple.take(2) + processArgs.mapPassthrough(tuple.drop(2))
        }

        // check tuple
        assert tuple instanceof List : 
          "Error in module '${workflowKey}': element in channel should be a tuple [id, data, ...otherargs...]\n" +
          "  Example: [\"id\", [input: file('foo.txt'), arg: 10]].\n" +
          "  Expected class: List. Found: tuple.getClass() is ${tuple.getClass()}"
        assert tuple.size() >= 2 : 
          "Error in module '${workflowKey}': expected length of tuple in input channel to be two or greater.\n" +
          "  Example: [\"id\", [input: file('foo.txt'), arg: 10]].\n" +
          "  Found: tuple.size() == ${tuple.size()}"
        
        // check id field
        assert tuple[0] instanceof String : 
          "Error in module '${workflowKey}': first element of tuple in channel should be a String\n" +
          "  Example: [\"id\", [input: file('foo.txt'), arg: 10]].\n" +
          "  Found: ${tuple[0]}"
        
        // match file to input file
        if (processArgs.simplifyInput && tuple[1] instanceof Path) {
          def inputFiles = thisFunctionality.arguments
            .findAll { it.type == "file" && it.direction == "input" }
          
          assert inputFiles.size() == 1 : 
              "Error in module '${workflowKey}' id '${tuple[0]}'.\n" +
              "  Anonymous file inputs are only allowed when the process has exactly one file input.\n" +
              "  Expected: inputFiles.size() == 1. Found: inputFiles.size() is ${inputFiles.size()}"

          tuple[1] = [[ inputFiles[0].name, tuple[1] ]].collectEntries()
        }

        // check data field
        assert tuple[1] instanceof Map : 
          "Error in module '${workflowKey}' id '${tuple[0]}': second element of tuple in channel should be a Map\n" +
          "  Example: [\"id\", [input: file('foo.txt'), arg: 10]].\n" +
          "  Expected class: Map. Found: tuple[1].getClass() is ${tuple[1].getClass()}"

        // rename keys of data field in tuple
        if (processArgs.renameKeys) {
          assert processArgs.renameKeys instanceof Map : 
              "Error renaming data keys in module '${workflowKey}' id '${tuple[0]}'.\n" +
              "  Example: renameKeys: ['new_key': 'old_key'].\n" +
              "  Expected class: Map. Found: renameKeys.getClass() is ${processArgs.renameKeys.getClass()}"
          assert tuple[1] instanceof Map : 
              "Error renaming data keys in module '${workflowKey}' id '${tuple[0]}'.\n" +
              "  Expected class: Map. Found: tuple[1].getClass() is ${tuple[1].getClass()}"

          // TODO: allow renameKeys to be a function?
          processArgs.renameKeys.each { newKey, oldKey ->
            assert newKey instanceof String : 
              "Error renaming data keys in module '${workflowKey}' id '${tuple[0]}'.\n" +
              "  Example: renameKeys: ['new_key': 'old_key'].\n" +
              "  Expected class of newKey: String. Found: newKey.getClass() is ${newKey.getClass()}"
            assert oldKey instanceof String : 
              "Error renaming data keys in module '${workflowKey}' id '${tuple[0]}'.\n" +
              "  Example: renameKeys: ['new_key': 'old_key'].\n" +
              "  Expected class of oldKey: String. Found: oldKey.getClass() is ${oldKey.getClass()}"
            assert tuple[1].containsKey(oldKey) : 
              "Error renaming data keys in module '${workflowKey}' id '${tuple[0]}'.\n" +
              "  Key '$oldKey' is missing in the data map. tuple[1].keySet() is '${tuple[1].keySet()}'"
            tuple[1].put(newKey, tuple[1][oldKey])
          }
          tuple[1].keySet().removeAll(processArgs.renameKeys.collect{ newKey, oldKey -> oldKey })
        }
        tuple
      }
      | debug(processArgs, "processed")
      | map { tuple ->
        def id = tuple[0]
        def data = tuple[1]
        def passthrough = tuple.drop(2)

        // fetch default params from functionality
        def defaultArgs = thisFunctionality.arguments
          .findAll { it.containsKey("default") }
          .collectEntries { [ it.name, it.default ] }

        // fetch overrides in params
        def paramArgs = thisFunctionality.arguments
          .findAll { par ->
            def argKey = workflowKey + "__" + par.name
            params.containsKey(argKey) && params[argKey] != "viash_no_value"
          }
          .collectEntries { [ it.name, params[workflowKey + "__" + it.name] ] }
        
        // fetch overrides in data
        def dataArgs = thisFunctionality.arguments
          .findAll { data.containsKey(it.name) }
          .collectEntries { [ it.name, data[it.name] ] }
        
        // combine params
        def combinedArgs = defaultArgs + paramArgs + processArgs.args + dataArgs

        // remove arguments with explicit null values
        combinedArgs.removeAll{it == null}

        // check whether required arguments exist
        thisFunctionality.arguments
          .forEach { par ->
            if (par.required) {
              assert combinedArgs.containsKey(par.name): "Argument ${par.name} is required but does not have a value"
            }
          }

        // TODO: check whether parameters have the right type

        // process input files separately
        def inputPaths = thisFunctionality.arguments
          .findAll { it.type == "file" && it.direction == "input" }
          .collect { par ->
            def val = combinedArgs.containsKey(par.name) ? combinedArgs[par.name] : []
            if (val == null) {
              []
            } else if (val instanceof List) {
              val
            } else if (val instanceof Path) {
              [ val ]
            } else {
              []
            }
          }.collect{ it.findAll{ it.exists() } }

        // remove input files
        def argsExclInputFiles = thisFunctionality.arguments
          .findAll { it.type != "file" || it.direction != "input" }
          .collectEntries { par ->
            def key = par.name
            def val = combinedArgs[key]
            if (par.multiple && val instanceof Collection) {
              val = val.join(par.multiple_sep)
            }
            if (par.direction == "output" && par.type == "file") {
              val = val.replaceAll('\\$id', id).replaceAll('\\$key', workflowKey)
            }
            [key, val]
          }

        [ id ] + inputPaths + [ argsExclInputFiles, passthrough, resourcesDir ]
      }
      | processObj
      | map { output ->
        def outputFiles = thisFunctionality.arguments
          .findAll { it.type == "file" && it.direction == "output" }
          .indexed()
          .collectEntries{ index, par ->
            out = output[index + 2]
            // strip dummy '.exitcode' file from output (see nextflow-io/nextflow#2678)
            if (!out instanceof List || out.size() <= 1) {
              if (par.multiple) {
                out = []
              } else {
                assert !par.required :
                    "Error in module '${workflowKey}' id '${output[0]}' argument '${par.name}'.\n" +
                    "  Required output file is missing"
                out = null
              }
            } else if (out.size() == 2 && !par.multiple) {
              out = out[1]
            } else {
              out = out.drop(1)
            }
            [ par.name, out ]
          }
        
        // drop null outputs
        outputFiles.removeAll{it.value == null}

        if (processArgs.simplifyOutput && outputFiles.size() == 1) {
          outputFiles = outputFiles.values()[0]
        }

        def out = [ output[0], outputFiles ]

        // passthrough additional items
        if (output[1]) {
          out.addAll(output[1])
        }

        out
      }
      | debug(processArgs, "output")

    emit:
    output_
  }

  return workflowInstance.cloneWithName(workflowKey)
}

// initialise standard workflow
myWfInstance = workflowFactory(key: thisFunctionality.name)

// add factory function
myWfInstance.metaClass.run = { args ->
  workflowFactory(args)
}
// add workflow to environment
ScriptMeta.current().addDefinition(myWfInstance)

// Implicit workflow for running this module standalone
workflow {
  if (params.containsKey("help") && params["help"]) {
    exit 0, thisHelpMessage
  }
  if (!params.containsKey("id")) {
    params.id = "run"
  }
  if (!params.containsKey("publishDir")) {
    params.publishDir = "./"
  }

  // fetch parameters
  def args = thisFunctionality.arguments
    .findAll { par -> params.containsKey(par.name) }
    .collectEntries { par ->
      if (par.type == "file" && par.direction == "input") {
        [ par.name, file(params[par.name]) ]
      } else {
        [ par.name, params[par.name] ]
      }
    }
          
  Channel.value([ params.id, args ])
    | view { "input: $it" }
    | myWfInstance.run(
        directives: [publishDir: params.publishDir]
      )
    | view { "output: $it" }
}