// depends on: thisConfig, thisScript, session?
def processFactory(Map processArgs) {
  // autodetect process key
  def wfKey = processArgs["key"]
  def procKeyPrefix = "${wfKey}_process"
  def meta = nextflow.script.ScriptMeta.current()
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
    .collect { ', path(viash_par_' + it.plainName + ', stageAs: "_viash_par/' + it.plainName + '_?/*")' }
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
      viash_par_contents = "(viash_par_${par.plainName} instanceof List ? viash_par_${par.plainName}.join(\"${par.multiple_sep}\") : viash_par_${par.plainName})"
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
  def assertStr = (processArgs.auto.publish == true) || processArgs.auto.transcript ? 
    """\nassert task.publishDir.size() > 0: "if auto.publish is true, params.publish_dir needs to be defined.\\n  Example: --publish_dir './output/'" """ :
    ""

  // generate process string
  def procStr = 
  """nextflow.enable.dsl=2
  |
  |process $procKey {$drctvStrs
  |input:
  |  tuple val(id)$inputPaths, val(args), path(resourcesDir, stageAs: ".viash_meta_resources")
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
  |# export VIASH_META_RESOURCES_DIR="\${resourcesDir.toRealPath().toAbsolutePath()}"
  |export VIASH_META_RESOURCES_DIR="\${resourcesDir}"
  |export VIASH_META_TEMP_DIR="${['docker', 'podman', 'charliecloud'].any{ it == workflow.containerEngine } ? '/tmp' : tmpDir}"
  |export VIASH_META_FUNCTIONALITY_NAME="${thisConfig.functionality.name}"
  |# export VIASH_META_EXECUTABLE="\\\$VIASH_META_RESOURCES_DIR/\\\$VIASH_META_FUNCTIONALITY_NAME"
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
  def ownerParams = new nextflow.script.ScriptBinding.ParamsMap()
  def binding = new nextflow.script.ScriptBinding().setParams(ownerParams)
  def module = new nextflow.script.IncludeDef.Module(name: procKey)
  def scriptParser = new nextflow.script.ScriptParser(session)
    .setModule(true)
    .setBinding(binding)
  scriptParser.scriptPath = nextflow.script.ScriptMeta.current().getScriptPath()
  def moduleScript = scriptParser.runScript(procStr)
    .getScript()

  // register module in meta
  meta.addModule(moduleScript, module.name, module.alias)

  // retrieve and return process from meta
  return meta.getProcess(procKey)
}
