////////////////////////////
// VDSL3 helper functions //
////////////////////////////

import nextflow.Nextflow
import nextflow.script.IncludeDef
import nextflow.script.ScriptBinding
import nextflow.script.ScriptMeta
import nextflow.script.ScriptParser

// retrieve resourcesDir here to make sure the correct path is found
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
  drctv = drctv.findAll{k, v -> v != null}

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
    assert drctv["afterScript"] instanceof CharSequence
  }

  /* DIRECTIVE beforeScript
    accepted examples:
    - "source /cluster/bin/setup"
  */
  if (drctv.containsKey("beforeScript")) {
    assert drctv["beforeScript"] instanceof CharSequence
  }

  /* DIRECTIVE cache
    accepted examples:
    - true
    - false
    - "deep"
    - "lenient"
  */
  if (drctv.containsKey("cache")) {
    assert drctv["cache"] instanceof CharSequence || drctv["cache"] instanceof Boolean
    if (drctv["cache"] instanceof CharSequence) {
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
    assert drctv["conda"] instanceof CharSequence
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
    assert drctv["container"] instanceof Map || drctv["container"] instanceof CharSequence
    if (drctv["container"] instanceof Map) {
      def m = drctv["container"]
      assertMapKeys(m, [ "registry", "image", "tag" ], ["image"], "container")
      def part1 = 
        System.getenv('OVERRIDE_CONTAINER_REGISTRY') ? System.getenv('OVERRIDE_CONTAINER_REGISTRY') + "/" : 
        params.containsKey("override_container_registry") ? params["override_container_registry"] + "/" : // todo: remove?
        m.registry ? m.registry + "/" : 
        ""
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
    assert drctv["containerOptions"] instanceof CharSequence
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
    assert drctv["disk"] instanceof CharSequence
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
    assert drctv["errorStrategy"] instanceof CharSequence
    assert drctv["errorStrategy"] in ["terminate", "finish", "ignore", "retry"] : "Unexpected value for errorStrategy"
  }

  /* DIRECTIVE executor
    accepted examples:
    - "local"
    - "sge"
  */
  if (drctv.containsKey("executor")) {
    assert drctv["executor"] instanceof CharSequence
    assert drctv["executor"] in ["local", "sge", "uge", "lsf", "slurm", "pbs", "pbspro", "moab", "condor", "nqsii", "ignite", "k8s", "awsbatch", "google-pipelines"] : "Unexpected value for executor"
  }

  /* DIRECTIVE machineType
    accepted examples:
    - "n1-highmem-8"
  */
  if (drctv.containsKey("machineType")) {
    assert drctv["machineType"] instanceof CharSequence
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
    assert drctv["memory"] instanceof CharSequence
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
    assert drctv["module"] instanceof CharSequence
  }

  /* DIRECTIVE penv
    accepted examples:
    - "smp"
  */
  if (drctv.containsKey("penv")) {
    assert drctv["penv"] instanceof CharSequence
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
    assert pblsh instanceof List || pblsh instanceof Map || pblsh instanceof CharSequence
    
    // turn into list if not already so
    // for some reason, 'if (!pblsh instanceof List) pblsh = [ pblsh ]' doesn't work.
    pblsh = pblsh instanceof List ? pblsh : [ pblsh ]

    // check elements of publishDir
    pblsh = pblsh.collect{ elem ->
      // turn into map if not already so
      elem = elem instanceof CharSequence ? [ path: elem ] : elem

      // check types and keys
      assert elem instanceof Map : "Expected publish argument '$elem' to be a String or a Map. Found: class ${elem.getClass()}"
      assertMapKeys(elem, [ "path", "mode", "overwrite", "pattern", "saveAs", "enabled" ], ["path"], "publishDir")

      // check elements in map
      assert elem.containsKey("path")
      assert elem["path"] instanceof CharSequence
      if (elem.containsKey("mode")) {
        assert elem["mode"] instanceof CharSequence
        assert elem["mode"] in [ "symlink", "rellink", "link", "copy", "copyNoFollow", "move" ]
      }
      if (elem.containsKey("overwrite")) {
        assert elem["overwrite"] instanceof Boolean
      }
      if (elem.containsKey("pattern")) {
        assert elem["pattern"] instanceof CharSequence
      }
      if (elem.containsKey("saveAs")) {
        assert elem["saveAs"] instanceof CharSequence //: "saveAs as a Closure is currently not supported. Surround your closure with single quotes to get the desired effect. Example: '\{ foo \}'"
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
    assert drctv["queue"] instanceof CharSequence
  }

  /* DIRECTIVE label
    accepted examples:
    - "big_mem"
    - "big_cpu"
    - ["big_mem", "big_cpu"]
  */
  if (drctv.containsKey("label")) {
    if (drctv["label"] instanceof CharSequence) {
      drctv["label"] = [ drctv["label"] ]
    }
    assert drctv["label"] instanceof List
    drctv["label"].forEach { label ->
      assert label instanceof CharSequence
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
    assert drctv["scratch"] == true || drctv["scratch"] instanceof CharSequence
  }

  /* DIRECTIVE storeDir
    accepted examples:
    - "/path/to/storeDir"
  */
  if (drctv.containsKey("storeDir")) {
    assert drctv["storeDir"] instanceof CharSequence
  }

  /* DIRECTIVE stageInMode
    accepted examples:
    - "copy"
    - "link"
  */
  if (drctv.containsKey("stageInMode")) {
    assert drctv["stageInMode"] instanceof CharSequence
    assert drctv["stageInMode"] in ["copy", "link", "symlink", "rellink"]
  }

  /* DIRECTIVE stageOutMode
    accepted examples:
    - "copy"
    - "link"
  */
  if (drctv.containsKey("stageOutMode")) {
    assert drctv["stageOutMode"] instanceof CharSequence
    assert drctv["stageOutMode"] in ["copy", "move", "rsync"]
  }

  /* DIRECTIVE tag
    accepted examples:
    - "foo"
    - '$id'
  */
  if (drctv.containsKey("tag")) {
    assert drctv["tag"] instanceof CharSequence
  }

  /* DIRECTIVE time
    accepted examples:
    - "1h"
    - "2days"
    - "1day 6hours 3minutes 30seconds"
  */
  if (drctv.containsKey("time")) {
    assert drctv["time"] instanceof CharSequence
    // todo: validation regex?
  }

  return drctv
}

// TODO: unit test processAuto
def processAuto(Map auto) {
  // remove null values
  auto = auto.findAll{k, v -> v != null}

  expectedKeys = ["simplifyInput", "simplifyOutput", "transcript", "publish"]

  // check whether expected keys are all booleans (for now)
  for (key in expectedKeys) {
    assert auto.containsKey(key)
    assert auto[key] instanceof Boolean
  }

  return auto.subMap(expectedKeys)
}

def processProcessArgs(Map args) {
  // override defaults with args
  def processArgs = thisDefaultProcessArgs + args

  // check whether 'key' exists
  assert processArgs.containsKey("key")

  // if 'key' is a closure, apply it to the original key
  if (processArgs["key"] instanceof Closure) {
    processArgs["key"] = processArgs["key"](thisConfig.functionality.name)
  }
  assert processArgs["key"] instanceof CharSequence
  assert processArgs["key"] ==~ /^[a-zA-Z_][a-zA-Z0-9_]*$/

  // check whether directives exists and apply defaults
  assert processArgs.containsKey("directives")
  assert processArgs["directives"] instanceof Map
  processArgs["directives"] = processDirectives(thisDefaultProcessArgs.directives + processArgs["directives"])

  // check whether directives exists and apply defaults
  assert processArgs.containsKey("auto")
  assert processArgs["auto"] instanceof Map
  processArgs["auto"] = processAuto(thisDefaultProcessArgs.auto + processArgs["auto"])

  // auto define publish, if so desired
  if (processArgs.auto.publish == true && (processArgs.directives.publishDir != null ? processArgs.directives.publishDir : [:]).isEmpty()) {
    // can't assert at this level thanks to the no_publish profile
    // assert params.containsKey("publishDir") || params.containsKey("publish_dir") : 
    //   "Error in module '${processArgs['key']}': if auto.publish is true, params.publish_dir needs to be defined.\n" +
    //   "  Example: params.publish_dir = \"./output/\""
    def publishDir = 
      params.containsKey("publish_dir") ? params.publish_dir : 
      params.containsKey("publishDir") ? params.publishDir : 
      null
    
    if (publishDir != null) {
      processArgs.directives.publishDir = [[ 
        path: publishDir, 
        saveAs: "{ it.startsWith('.') ? null : it }", // don't publish hidden files, by default
        mode: "copy"
      ]]
    }
  }

  // auto define transcript, if so desired
  if (processArgs.auto.transcript == true) {
    // can't assert at this level thanks to the no_publish profile
    // assert params.containsKey("transcriptsDir") || params.containsKey("transcripts_dir") || params.containsKey("publishDir") || params.containsKey("publish_dir") : 
    //   "Error in module '${processArgs['key']}': if auto.transcript is true, either params.transcripts_dir or params.publish_dir needs to be defined.\n" +
    //   "  Example: params.transcripts_dir = \"./transcripts/\""
    def transcriptsDir = 
      params.containsKey("transcripts_dir") ? params.transcripts_dir : 
      params.containsKey("transcriptsDir") ? params.transcriptsDir : 
      params.containsKey("publish_dir") ? params.publish_dir + "/_transcripts" :
      params.containsKey("publishDir") ? params.publishDir + "/_transcripts" : 
      null
    if (transcriptsDir != null) {
      def timestamp = Nextflow.getSession().getWorkflowMetadata().start.format('yyyy-MM-dd_HH-mm-ss')
      def transcriptsPublishDir = [ 
        path: "$transcriptsDir/$timestamp/\${task.process.replaceAll(':', '-')}/\${id}/",
        saveAs: "{ it.startsWith('.') ? it.replaceAll('^.', '') : null }", 
        mode: "copy"
      ]
      def publishDirs = processArgs.directives.publishDir != null ? processArgs.directives.publishDir : null ? processArgs.directives.publishDir : []
      processArgs.directives.publishDir = publishDirs + transcriptsPublishDir
    }
  }

  // if this is a stubrun, remove certain directives?
  if (workflow.stubRun) {
    processArgs.directives.keySet().removeAll(["publishDir", "cpus", "memory", "label"])
  }

  for (nam in [ "map", "mapId", "mapData", "mapPassthrough", "filter" ]) {
    if (processArgs.containsKey(nam) && processArgs[nam]) {
      assert processArgs[nam] instanceof Closure : "Expected process argument '$nam' to be null or a Closure. Found: class ${processArgs[nam].getClass()}"
    }
  }

  // return output
  return processArgs
}

def processFactory(Map processArgs) {
  // autodetect process key
  def wfKey = processArgs["key"]
  def procKeyPrefix = "${wfKey}_process"
  def meta = ScriptMeta.current()
  def existing = meta.getProcessNames().findAll{it.startsWith(procKeyPrefix)}
  def numbers = existing.collect{it.replace(procKeyPrefix, "0").toInteger()}
  def newNumber = (numbers + [-1]).max() + 1

  def procKey = newNumber == 0 ? procKeyPrefix : "$procKeyPrefix$newNumber"

  if (newNumber > 0) {
    log.warn "Key for module '${wfKey}' is duplicated.\n",
      "If you run a component multiple times in the same workflow,\n" +
      "it's recommended you set a unique key for every call,\n" +
      "for example: ${wfKey}.run(key: \"foo\")."
  }

  // subset directives and convert to list of tuples
  def drctv = processArgs.directives

  // TODO: unit test the two commands below
  // convert publish array into tags
  def valueToStr = { val ->
    // ignore closures
    if (val instanceof CharSequence) {
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
          "\n$key " + val.collect{ k, v -> k + ": " + valueToStr(v) }.join(", ")
        } else if (val == null) {
          ""
        } else {
          "\n$key " + valueToStr(val)
        }
      }.join()
    } else if (value instanceof Map) {
      "\n$key " + value.collect{ k, v -> k + ": " + valueToStr(v) }.join(", ")
    } else {
      "\n$key " + valueToStr(value)
    }
  }.join()

  def inputPaths = thisConfig.functionality.allArguments
    .findAll { it.type == "file" && it.direction == "input" }
    .collect { ', path(viash_par_' + it.plainName + ')' }
    .join()

  def outputPaths = thisConfig.functionality.allArguments
    .findAll { it.type == "file" && it.direction == "output" }
    .collect { par ->
      // insert dummy into every output (see nextflow-io/nextflow#2678)
      if (!par.multiple) {
        ', path{[".exitcode", args.' + par.plainName + ']}'
      } else {
        ', path{[".exitcode"] + args.' + par.plainName + '}'
      }
    }
    .join()

  // TODO: move this functionality somewhere else?
  if (processArgs.auto.transcript) {
    outputPaths = outputPaths + ', path{[".exitcode", ".command*"]}'
  } else {
    outputPaths = outputPaths + ', path{[".exitcode"]}'
  }

  // create dirs for output files (based on BashWrapper.createParentFiles)
  def createParentStr = thisConfig.functionality.allArguments
    .findAll { it.type == "file" && it.direction == "output" && it.create_parent }
    .collect { par -> 
      "\${ args.containsKey(\"${par.plainName}\") ? \"mkdir_parent \\\"\" + (args[\"${par.plainName}\"] instanceof String ? args[\"${par.plainName}\"] : args[\"${par.plainName}\"].join('\" \"')) + \"\\\"\" : \"\" }"
    }
    .join("\n")

  // construct inputFileExports
  def inputFileExports = thisConfig.functionality.allArguments
    .findAll { it.type == "file" && it.direction.toLowerCase() == "input" }
    .collect { par ->
      viash_par_contents = !par.required && !par.multiple ? "viash_par_${par.plainName}[0]" : "viash_par_${par.plainName}.join(\"${par.multiple_sep}\")"
      "\n\${viash_par_${par.plainName}.empty ? \"\" : \"export VIASH_PAR_${par.plainName.toUpperCase()}=\\\"\" + ${viash_par_contents} + \"\\\"\"}"
    }

  // NOTE: if using docker, use /tmp instead of tmpDir!
  def tmpDir = java.nio.file.Paths.get(
    System.getenv('NXF_TEMP') ?: 
    System.getenv('VIASH_TEMP') ?: 
    System.getenv('VIASH_TMPDIR') ?: 
    System.getenv('VIASH_TEMPDIR') ?: 
    System.getenv('VIASH_TMP') ?: 
    System.getenv('TEMP') ?: 
    System.getenv('TMPDIR') ?: 
    System.getenv('TEMPDIR') ?:
    System.getenv('TMP') ?: 
    '/tmp'
  ).toAbsolutePath()

  // construct stub
  def stub = thisConfig.functionality.allArguments
    .findAll { it.type == "file" && it.direction == "output" }
    .collect { par -> 
      "\${ args.containsKey(\"${par.plainName}\") ? \"touch2 \\\"\" + (args[\"${par.plainName}\"] instanceof String ? args[\"${par.plainName}\"].replace(\"_*\", \"_0\") : args[\"${par.plainName}\"].join('\" \"')) + \"\\\"\" : \"\" }"
    }
    .join("\n")

  // escape script
  def escapedScript = thisScript.replace('\\', '\\\\').replace('$', '\\$').replace('"""', '\\"\\"\\"')

  // publishdir assert
  def assertStr = processArgs.auto.publish || processArgs.auto.transcript ? 
    """\nassert task.publishDir.size() > 0: "if auto.publish is true, params.publish_dir needs to be defined.\\n  Example: --publish_dir './output/'" """ :
    ""

  // generate process string
  def procStr = 
  """nextflow.enable.dsl=2
  |
  |process $procKey {$drctvStrs
  |input:
  |  tuple val(id)$inputPaths, val(args), path(resourcesDir)
  |output:
  |  tuple val("\$id")$outputPaths, optional: true
  |stub:
  |\"\"\"
  |touch2() { mkdir -p "\\\$(dirname "\\\$1")" && touch "\\\$1" ; }
  |$stub
  |\"\"\"
  |script:$assertStr
  |def escapeText = { s -> s.toString().replaceAll('([`"])', '\\\\\\\\\$1') }
  |def parInject = args
  |  .findAll{key, value -> value != null}
  |  .collect{key, value -> "export VIASH_PAR_\${key.toUpperCase()}=\\\"\${escapeText(value)}\\\""}
  |  .join("\\n")
  |\"\"\"
  |# meta exports
  |export VIASH_META_RESOURCES_DIR="\${resourcesDir.toRealPath().toAbsolutePath()}"
  |export VIASH_META_TEMP_DIR="${['docker', 'podman', 'charliecloud'].any{ it == workflow.containerEngine } ? '/tmp' : tmpDir}"
  |export VIASH_META_FUNCTIONALITY_NAME="${thisConfig.functionality.name}"
  |export VIASH_META_EXECUTABLE="\\\$VIASH_META_RESOURCES_DIR/\\\$VIASH_META_FUNCTIONALITY_NAME"
  |export VIASH_META_CONFIG="\\\$VIASH_META_RESOURCES_DIR/.config.vsh.yaml"
  |\${task.cpus ? "export VIASH_META_CPUS=\$task.cpus" : "" }
  |\${task.memory?.bytes != null ? "export VIASH_META_MEMORY_B=\$task.memory.bytes" : "" }
  |if [ ! -z \\\${VIASH_META_MEMORY_B+x} ]; then
  |  export VIASH_META_MEMORY_KB=\\\$(( (\\\$VIASH_META_MEMORY_B+1023) / 1024 ))
  |  export VIASH_META_MEMORY_MB=\\\$(( (\\\$VIASH_META_MEMORY_KB+1023) / 1024 ))
  |  export VIASH_META_MEMORY_GB=\\\$(( (\\\$VIASH_META_MEMORY_MB+1023) / 1024 ))
  |  export VIASH_META_MEMORY_TB=\\\$(( (\\\$VIASH_META_MEMORY_GB+1023) / 1024 ))
  |  export VIASH_META_MEMORY_PB=\\\$(( (\\\$VIASH_META_MEMORY_TB+1023) / 1024 ))
  |fi
  |
  |# meta synonyms
  |export VIASH_TEMP="\\\$VIASH_META_TEMP_DIR"
  |export TEMP_DIR="\\\$VIASH_META_TEMP_DIR"
  |
  |# create output dirs if need be
  |function mkdir_parent {
  |  for file in "\\\$@"; do 
  |    mkdir -p "\\\$(dirname "\\\$file")"
  |  done
  |}
  |$createParentStr
  |
  |# argument exports${inputFileExports.join()}
  |\$parInject
  |
  |# process script
  |${escapedScript}
  |\"\"\"
  |}
  |""".stripMargin()

  // TODO: print on debug
  // if (processArgs.debug == true) {
  //   println("######################\n$procStr\n######################")
  // }

  // create runtime process
  def ownerParams = new ScriptBinding.ParamsMap()
  def binding = new ScriptBinding().setParams(ownerParams)
  def module = new IncludeDef.Module(name: procKey)
  def scriptParser = new ScriptParser(session)
    .setModule(true)
    .setBinding(binding)
  scriptParser.scriptPath = ScriptMeta.current().getScriptPath()
  def moduleScript = scriptParser.runScript(procStr)
    .getScript()

  // register module in meta
  meta.addModule(moduleScript, module.name, module.alias)

  // retrieve and return process from meta
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
  def key = processArgs["key"]
  def meta = ScriptMeta.current()

  def workflowKey = key

  def processObj = null
  
  workflow workflowInstance {
    take:
    input_

    main:
    if (processObj == null) {
      processObj = processFactory(processArgs)
    }

    mid1_ = input_
      | debug(processArgs, "input")
      | map { tuple ->
        tuple = tuple.clone()
        
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
          "Error in module '${key}': element in channel should be a tuple [id, data, ...otherargs...]\n" +
          "  Example: [\"id\", [input: file('foo.txt'), arg: 10]].\n" +
          "  Expected class: List. Found: tuple.getClass() is ${tuple.getClass()}"
        assert tuple.size() >= 2 : 
          "Error in module '${key}': expected length of tuple in input channel to be two or greater.\n" +
          "  Example: [\"id\", [input: file('foo.txt'), arg: 10]].\n" +
          "  Found: tuple.size() == ${tuple.size()}"
        
        // check id field
        assert tuple[0] instanceof CharSequence : 
          "Error in module '${key}': first element of tuple in channel should be a String\n" +
          "  Example: [\"id\", [input: file('foo.txt'), arg: 10]].\n" +
          "  Found: ${tuple[0]}"
        
        // match file to input file
        if (processArgs.auto.simplifyInput && (tuple[1] instanceof Path || tuple[1] instanceof List)) {
          def inputFiles = thisConfig.functionality.allArguments
            .findAll { it.type == "file" && it.direction == "input" }
          
          assert inputFiles.size() == 1 : 
              "Error in module '${key}' id '${tuple[0]}'.\n" +
              "  Anonymous file inputs are only allowed when the process has exactly one file input.\n" +
              "  Expected: inputFiles.size() == 1. Found: inputFiles.size() is ${inputFiles.size()}"

          tuple[1] = [[ inputFiles[0].plainName, tuple[1] ]].collectEntries()
        }

        // check data field
        assert tuple[1] instanceof Map : 
          "Error in module '${key}' id '${tuple[0]}': second element of tuple in channel should be a Map\n" +
          "  Example: [\"id\", [input: file('foo.txt'), arg: 10]].\n" +
          "  Expected class: Map. Found: tuple[1].getClass() is ${tuple[1].getClass()}"

        // rename keys of data field in tuple
        if (processArgs.renameKeys) {
          assert processArgs.renameKeys instanceof Map : 
              "Error renaming data keys in module '${key}' id '${tuple[0]}'.\n" +
              "  Example: renameKeys: ['new_key': 'old_key'].\n" +
              "  Expected class: Map. Found: renameKeys.getClass() is ${processArgs.renameKeys.getClass()}"
          assert tuple[1] instanceof Map : 
              "Error renaming data keys in module '${key}' id '${tuple[0]}'.\n" +
              "  Expected class: Map. Found: tuple[1].getClass() is ${tuple[1].getClass()}"

          // TODO: allow renameKeys to be a function?
          processArgs.renameKeys.each { newKey, oldKey ->
            assert newKey instanceof CharSequence : 
              "Error renaming data keys in module '${key}' id '${tuple[0]}'.\n" +
              "  Example: renameKeys: ['new_key': 'old_key'].\n" +
              "  Expected class of newKey: String. Found: newKey.getClass() is ${newKey.getClass()}"
            assert oldKey instanceof CharSequence : 
              "Error renaming data keys in module '${key}' id '${tuple[0]}'.\n" +
              "  Example: renameKeys: ['new_key': 'old_key'].\n" +
              "  Expected class of oldKey: String. Found: oldKey.getClass() is ${oldKey.getClass()}"
            assert tuple[1].containsKey(oldKey) : 
              "Error renaming data keys in module '${key}' id '${tuple[0]}'.\n" +
              "  Key '$oldKey' is missing in the data map. tuple[1].keySet() is '${tuple[1].keySet()}'"
            tuple[1].put(newKey, tuple[1][oldKey])
          }
          tuple[1].keySet().removeAll(processArgs.renameKeys.collect{ newKey, oldKey -> oldKey })
        }
        tuple
      }

    if (processArgs.filter) {
      mid2_ = mid1_
        | filter{processArgs.filter(it)}
    } else {
      mid2_ = mid1_
    }

    if (processArgs.fromState) {
      // TODO: allow `fromState` to be a list[string] or map[string, string]
      // TODO: if `fromState` is a closure, check if it has exactly two arguments?
      mid3_ = mid2_
        | map{
          def id = it[0]
          def state = it[1]
          def data = processArgs.fromState(id, state)
          [id, data]
        }
    } else {
      mid3_ = mid2_
    }

    out0_ = mid3_
      | debug(processArgs, "processed")
      | map { tuple ->
        def id = tuple[0]
        def data = tuple[1]

        // fetch default params from functionality
        def defaultArgs = thisConfig.functionality.allArguments
          .findAll { it.containsKey("default") }
          .collectEntries { [ it.plainName, it.default ] }

        // fetch overrides in params
        def paramArgs = thisConfig.functionality.allArguments
          .findAll { par ->
            def argKey = key + "__" + par.plainName
            params.containsKey(argKey) && params[argKey] != "viash_no_value"
          }
          .collectEntries { [ it.plainName, params[key + "__" + it.plainName] ] }
        
        // fetch overrides in data
        def dataArgs = thisConfig.functionality.allArguments
          .findAll { data.containsKey(it.plainName) }
          .collectEntries { [ it.plainName, data[it.plainName] ] }
        
        // combine params
        def combinedArgs = defaultArgs + paramArgs + processArgs.args + dataArgs

        // remove arguments with explicit null values
        combinedArgs.removeAll{it.value == null}

        if (workflow.stubRun) {
          // add id if missing
          combinedArgs = [id: 'stub'] + combinedArgs
        } else {
          // check whether required arguments exist
          thisConfig.functionality.allArguments
            .forEach { par ->
              if (par.required) {
                assert combinedArgs.containsKey(par.plainName): "Argument ${par.plainName} is required but does not have a value"
              }
            }
        }

        // TODO: check whether parameters have the right type

        // process input files separately
        def inputPaths = thisConfig.functionality.allArguments
          .findAll { it.type == "file" && it.direction == "input" }
          .collect { par ->
            def val = combinedArgs.containsKey(par.plainName) ? combinedArgs[par.plainName] : []
            def inputFiles = []
            if (val == null) {
              inputFiles = []
            } else if (val instanceof List) {
              inputFiles = val
            } else if (val instanceof Path) {
              inputFiles = [ val ]
            } else {
              inputFiles = []
            }
            if (!workflow.stubRun) {
              // throw error when an input file doesn't exist
              inputFiles.each{ file -> 
                assert file.exists() :
                  "Error in module '${key}' id '${id}' argument '${par.plainName}'.\n" +
                  "  Required input file does not exist.\n" +
                  "  Path: '$file'.\n" +
                  "  Expected input file to exist"
              }
            }
            inputFiles 
          } 

        // remove input files
        def argsExclInputFiles = thisConfig.functionality.allArguments
          .findAll { (it.type != "file" || it.direction != "input") && combinedArgs.containsKey(it.plainName) }
          .collectEntries { par ->
            def parName = par.plainName
            def val = combinedArgs[parName]
            if (par.multiple && val instanceof Collection) {
              val = val.join(par.multiple_sep)
            }
            if (par.direction == "output" && par.type == "file") {
              val = val.replaceAll('\\$id', id).replaceAll('\\$key', key)
            }
            [parName, val]
          }

        [ id ] + inputPaths + [ argsExclInputFiles, resourcesDir ]
      }
      | processObj
      | map { output ->
        def outputFiles = thisConfig.functionality.allArguments
          .findAll { it.type == "file" && it.direction == "output" }
          .indexed()
          .collectEntries{ index, par ->
            out = output[index + 1]
            // strip dummy '.exitcode' file from output (see nextflow-io/nextflow#2678)
            if (!out instanceof List || out.size() <= 1) {
              if (par.multiple) {
                out = []
              } else {
                assert !par.required :
                    "Error in module '${key}' id '${output[0]}' argument '${par.plainName}'.\n" +
                    "  Required output file is missing"
                out = null
              }
            } else if (out.size() == 2 && !par.multiple) {
              out = out[1]
            } else {
              out = out.drop(1)
            }
            [ par.plainName, out ]
          }
        
        // drop null outputs
        outputFiles.removeAll{it.value == null}

        if (processArgs.auto.simplifyOutput && outputFiles.size() == 1) {
          outputFiles = outputFiles.values()[0]
        }

        [ output[0], outputFiles ]
      }

    // join the output [id, output] with the previous state [id, state, ...]
    out1_ = out0_.join(mid2_, failOnDuplicate: true)
      // input tuple format: [id, output, prev_state, ...]
      // output tuple format: [id, new_state, ...]
      | map{
        // TODO: allow `toState` to be a list[string] or map[string, string]
        // TODO: if `toState` is a closure, check if it has exactly two arguments?
        def new_state = processArgs.toState(it)
        [it[0], new_state] + it.drop(3)
      }
      | debug(processArgs, "output")


    emit:
    out1_
  }

  def wf = workflowInstance.cloneWithName(workflowKey)

  // add factory function
  wf.metaClass.run = { runArgs ->
    workflowFactory(runArgs)
  }
  // add config to module for later introspection
  wf.metaClass.config = thisConfig

  return wf
}

// initialise default workflow
myWfInstance = workflowFactory([:])

// add workflow to environment
ScriptMeta.current().addDefinition(myWfInstance)

// anonymous workflow for running this module as a standalone
workflow {
  def mergedConfig = thisConfig
  def mergedParams = [:] + params

  // add id argument if it's not already in the config
  if (mergedConfig.functionality.arguments.every{it.plainName != "id"}) {
    def idArg = [
      'name': '--id',
      'required': false,
      'type': 'string',
      'description': 'A unique id for every entry.',
      'multiple': false
    ]
    mergedConfig.functionality.arguments.add(0, idArg)
    mergedConfig = processConfig(mergedConfig)
  }
  if (!mergedParams.containsKey("id")) {
    mergedParams.id = "run"
  }

  helpMessage(mergedConfig)

  channelFromParams(mergedParams, mergedConfig)
    | preprocessInputs("config": mergedConfig)
    | view { "input: $it" }
    | myWfInstance.run(
      auto: [ publish: true ]
    )
    | view { "output: $it" }
}