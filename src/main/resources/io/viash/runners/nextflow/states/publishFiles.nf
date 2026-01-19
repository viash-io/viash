def _publishingProcessFactory(String wfKey) {
  // autodetect process key
  def procKeyPrefix = "PublishFiles_${wfKey}"
  def scriptMeta = nextflow.script.ScriptMeta.current()
  def existing = scriptMeta.getProcessNames().findAll{it.startsWith(procKeyPrefix)}
  def numbers = existing.collect{it.replace(procKeyPrefix, "0").toInteger()}
  def newNumber = (numbers + [-1]).max() + 1

  def procKey = newNumber == 0 ? procKeyPrefix : "$procKeyPrefix$newNumber"

  if (newNumber > 0) {
    log.warn "Key for module '${wfKey}' is duplicated.\n",
      "If you run a component multiple times in the same workflow,\n" +
      "it's recommended you set a unique key for every call,\n" +
      "for example: ${wfKey}.run(key: \"foo\")."
  }

  def publishDir = getPublishDir()

  // generate process string
  def procStr = 
  """nextflow.enable.dsl=2
  |
  |process $procKey {
  |publishDir path: "$publishDir", mode: "copy"
  |tag "\$id"
  |input:
  |  tuple val(id), path(inputFiles, stageAs: "_inputfile?/*"), val(outputFiles)
  |output:
  |  tuple val(id), path{outputFiles}
  |script:
  |def copyCommands = [
  | inputFiles instanceof List ? inputFiles : [inputFiles],
  | outputFiles instanceof List ? outputFiles : [outputFiles]
  |]
  |  .transpose()
  |  .collectMany{infile, outfile ->
  |    if (infile.toString() != outfile.toString()) {
  |      [
  |        "[ -d \\"\\\$(dirname '\${outfile.toString()}')\\" ] || mkdir -p \\"\\\$(dirname '\${outfile.toString()}')\\"",
  |        "cp -r '\${infile.toString()}' '\${outfile.toString()}'"
  |      ]
  |    } else {
  |      // no need to copy if infile is the same as outfile
  |      []
  |    }
  |  }
  |
  |\"\"\"
  |echo "Copying output files to destination folder"
  |\${copyCommands.join("\\n  ")}
  |\"\"\"
  |}
  |""".stripMargin()

  // write process to temp file
  def tempFile = java.nio.file.Files.createTempFile("viash-process-${procKey}-", ".nf")
  // addShutdownHook { java.nio.file.Files.deleteIfExists(tempFile) }
  tempFile.text = procStr

  // create process from temp file
  def binding = new nextflow.script.ScriptBinding([:])
  def session = nextflow.Nextflow.getSession()
  def parser = new nextflow.script.ScriptParser(session)
    .setModule(true)
    .setBinding(binding)
  def moduleScript = parser.runScript(tempFile)
    .getScript()

  // register module in meta
  def module = new nextflow.script.IncludeDef.Module(name: procKey)
  scriptMeta.addModule(moduleScript, module.name, module.alias)

  // retrieve and return process from meta
  return scriptMeta.getProcess(procKey)
}

def processStateForPar(par, id_, key_, state_, origState_) {
  def plainName_ = par.plainName
  // if the state does not contain the key, it's an
  // optional argument for which the component did 
  // not generate any output OR multiple channels were emitted
  // and the output was just not added to using the channel
  // that is now being parsed
  if (!state_.containsKey(plainName_)) {
    return []
  }
  def value = state_[plainName_]
  // if the orig state does not contain this filename,
  // it's an optional argument for which the user specified
  // that it should not be returned as a state
  if (!origState_.containsKey(plainName_)) {
    return []
  }
  def filenameTemplate = origState_[plainName_]
  // if the pararameter is multiple: true, fetch the template
  if (par.multiple && filenameTemplate instanceof List) {
    filenameTemplate = filenameTemplate[0]
  }
  // instantiate the template
  def filename = filenameTemplate
    .replaceAll('\\$id', id_)
    .replaceAll('\\$\\{id\\}', id_)
    .replaceAll('\\$key', key_)
    .replaceAll('\\$\\{key\\}', key_)
  
  if (par.multiple) {
    // if the parameter is multiple: true, the filename
    // should contain a wildcard '*' that is replaced with
    // the index of the file
    assert filename.contains("*") : "Module '${key_}' id '${id_}': Multiple output files specified, but no wildcard '*' in the filename: ${filename}"
    def outputPerFile = value.withIndex().collect{ val, ix ->
      def filename_ix = filename.replace("*", ix.toString())
      def inputPath = val instanceof File ? val.toPath() : val
      [inputPath: inputPath, outputFilename: filename_ix]
    }
    def transposedOutputs = ["inputPath", "outputFilename"].collectEntries{ key -> 
      [key, outputPerFile.collect{dic -> dic[key]}]
    }
    return [[key: plainName_] + transposedOutputs]
  }

  def value_ = java.nio.file.Paths.get(filename)
  def inputPath = value instanceof File ? value.toPath() : value
  return [[inputPath: [inputPath], outputFilename: [filename]]]

}

// this assumes that the state contains no other values other than those specified in the config
def publishFilesByConfig(Map args) {
  def parameter_info = args.get("par")
  assert parameter_info != null : "publishFilesByConfig: par must be specified"
  def key_ = args.get("key")
  assert key_ != null : "publishFilesByConfig: key must be specified"
  
  workflow publishFilesSimpleWf {
    take: input_ch
    main:
      input_ch
        | map { tup ->
          def id_ = tup[0]
          def state_ = tup[1] // e.g. [output: new File("myoutput.h5ad"), k: 10]
          def origState_ = tup[2] // e.g. [output: '$id.$key.foo.h5ad']
          
          // the processed state is a list of [key, inputPath, outputFilename] tuples, where
          //   - key is a String
          //   - inputPath is a List[Path]
          //   - outputFilename is a List[String]
          //   - (inputPath, outputFilename) are the files that will be copied from src to dest
          def processedState = processStateForPar(parameter_info, id_, key_, state_, origState_) 
          def inputPaths = processedState.collectMany{it.inputPath}
          def outputFilenames = processedState.collectMany{it.outputFilename}
          [id_, inputPaths, outputFilenames]
        }
        | _publishingProcessFactory(parameter_info.plainName)
    emit: input_ch
  }
  return publishFilesSimpleWf
}



