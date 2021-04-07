nextflow.enable.dsl=2

def renderCLI(command, arguments) {

    def argumentsList = arguments.collect{ it ->
        (it.otype == "")
            ? "\'" + it.value + "\'"
            : (it.type == "boolean_true")
                ? it.otype + it.name
                : (it.value == "")
                    ? ""
                    : it.otype + it.name + " \'" + ((it.value in List && it.multiple) ? it.value.join(it.multiple_sep): it.value) + "\'"
    }

    def command_line = command + argumentsList

    return command_line.join(" ")
}

def effectiveContainer(processParams) {
    def _registry = params.containsKey("containerRegistry") ? params.containerRegistry : processParams.containerRegistry
    def _name = processParams.container
    def _tag = params.containsKey("containerTag") ? params.containerTag : processParams.containerTag

    return (_registry == "" ? "" : _registry + "/") + _name + ":" + _tag
}

// Convert the nextflow.config arguments list to a List instead of a LinkedHashMap
// The rest of this main.nf script uses the Map form
def argumentsAsList(_params) {
    def overrideArgs = _params.arguments.collect{ key, value -> value }
    def newParams = _params + [ "arguments" : overrideArgs ]
    return newParams
}

// Use the params map, create a hashmap of the filenames for output
// output filename is <sample>.<method>.<arg_name>[.extension]
def outFromIn(_params) {

    def id = _params.id

   _params
        .arguments
        .findAll{ it -> it.type == "file" && it.direction == "Output" }
        .collect{ it ->
            // If a default (dflt) attribute is present, strip the extension from the filename,
            // otherwise just use the option name as an extension.
            def extOrName = (it.dflt != null) ? it.dflt.split(/\./).last() : it.name
            // The output filename is <sample> . <modulename> . <extension>
            def newName = id + "." + "testing" + "." + extOrName
            it + [ value : newName ]
        }

}

// In: Hashmap key -> DataObjects
// Out: Arrays of DataObjects
def overrideIO(_params, inputs, outputs) {

    // `inputs` in fact can be one of:
    // - `String`,
    // - `List[String]`,
    // - `Map[String, String | List[String]]`
    // Please refer to the docs for more info
    def overrideArgs = _params.arguments.collect{ it ->
        if (it.type == "file") {
            if (it.direction == "Input") {
                (inputs in List || inputs in HashMap)
                    ? (inputs in List)
                        ? it + [ "value" : inputs.join(it.multiple_sep)]
                        : (inputs[it.name] != null)
                            ? (inputs[it.name] in List)
                                ? it + [ "value" : inputs[it.name].join(it.multiple_sep)]
                                : it + [ "value" : inputs[it.name]]
                            : it
                    : it + [ "value" : inputs ]
            } else {
                (outputs in List || outputs in HashMap)
                    ? (outputs in List)
                        ? it + [ "value" : outputs.join(it.multiple_sep)]
                        : (outputs[it.name] != null)
                            ? (outputs[it.name] in List)
                                ? it + [ "value" : outputs[it.name].join(it.multiple_sep)]
                                : it + [ "value" : outputs[it.name]]
                            : it
                    : it + [ "value" : outputs ]
            }
        } else {
            it
        }
    }

    def newParams = _params + [ "arguments" : overrideArgs ]

    return newParams

}

process testing_process {

  tag "${id}"
  echo { (params.debug == true) ? true : false }
  cache 'deep'
  stageInMode "symlink"
  container "${container}"
  publishDir "out"

  input:
    tuple val(id), path(input), val(output), val(container), val(cli), val(_params)
  output:
    tuple val("${id}"), path(output), val(_params)
  script:
    if (params.test)
        """
        # Some useful stuff
        export NUMBA_CACHE_DIR=/tmp/numba-cache
        # Running the pre-hook when necessary
        echo Nothing before
        # Adding NXF's `$moduleDir` to the path in order to resolve our own wrappers
        export PATH="./:${moduleDir}:\$PATH"
        ./${params.testing.tests.testScript} | tee $output
        """
    else
        """
        # Some useful stuff
        export NUMBA_CACHE_DIR=/tmp/numba-cache
        # Running the pre-hook when necessary
        echo Nothing before
        # Adding NXF's `$moduleDir` to the path in order to resolve our own wrappers
        export PATH="${moduleDir}:\$PATH"
        $cli
        """
}

workflow testing {

    take:
    id_input_params_

    main:

    def key = "testing"

    def id_input_output_function_cli_ =
        id_input_params_.map{ id, input, _params ->

            // Start from the (global) params and overwrite with the (local) _params
            def defaultParams = params[key] ? params[key] : [:]
            def overrideParams = _params[key] ? _params[key] : [:]
            def updtParams = defaultParams + overrideParams
            // Convert to List[Map] for the arguments
            def newParams = argumentsAsList(updtParams) + [ "id" : id ]

            // Generate output filenames, out comes a Map
            def output = outFromIn(newParams)

            // The process expects Path or List[Path], Maps need to be converted
            def inputsForProcess =
                (input in HashMap)
                    ? input.collect{ k, v -> v }.flatten()
                    : input
            def outputsForProcess = output.collect{ it.value }

            // For our machinery, we convert Path -> String in the input
            def inputs =
                (input in List || input in HashMap)
                    ? (input in List)
                        ? input.collect{ it.name }
                        : input.collectEntries{ k, v -> [ k, (v in List) ? v.collect{it.name} : v.name ] }
                    : input.name
            outputs = output.collectEntries{ [(it.name): it.value] }

            def finalParams = overrideIO(newParams, inputs, outputs)

            new Tuple6(
                id,
                inputsForProcess,
                outputsForProcess,
                effectiveContainer(finalParams),
                renderCLI([finalParams.command], finalParams.arguments),
                finalParams
            )
        }

    result_ = testing_process(id_input_output_function_cli_) \
        | join(id_input_params_) \
        | map{ id, output, _params, input, original_params ->
            def parsedOutput = _params.arguments
                .findAll{ it.type == "file" && it.direction == "Output" }
                .withIndex()
                .collectEntries{ it, i ->
                    [(it.name): output[i]]
                }
            new Tuple3(id, parsedOutput, original_params)
        }

    emit:
    result_

}

workflow {

   def id = params.id
   def ch_ = Channel.fromPath(params.input).map{ s -> new Tuple3(id, s, params)}

   def criteria = multiMapCriteria {
        output: [ it[0], it[1]["output"] ]
        log: [ it[0], it[1]["log"] ]
   }

   def result =
       testing(ch_) \
            | view{ [ it[0], it[1] ] } \
            | multiMap(criteria)

    result.output.view{ "output: " + it[1] }
    result.log.view{ "log: " + it[1] }
}

workflow test {

   take:
   rootDir

   main:
   params.test = true
   params.testing.output = "testing.log"

   Channel.from(rootDir) \
        | filter { params.testing.tests.isDefined } \
        | map{ p -> new Tuple3(
                    "tests",
                    params.testing.tests.testResources.collect{ file( p + it ) },
                    params
                )} \
        | testing

    emit:
    testing.out
}

workflow overrideIOTest {

    def asMap = argumentsAsList(params.testing)

    def base = overrideIO(asMap, [], [])

    def input1 = "fileabcd.txt"
    def processed1 = overrideIO(asMap, input1, []).arguments
    assert processed1.findAll{ it.name == "input" }[0].value == input1

    // Passing an array means multiple options
    def input2 = [ "file1.txt", "file2.txt" ]
    def processed2 = overrideIO(asMap, input2, []).arguments
    assert processed2.findAll{ it.name == "input" }[0].value == input2.join(":")

    // The input2 key does not occur in the arguments list, so is omitted
    def input3 = [ input: "file1.txt", input2: "file2.txt" ]
    def processed3 = overrideIO(asMap, input3, []).arguments
    assert processed3.findAll{ it.name == "input" }[0].value == input3.input

    // The input key is an array
    def input4 = [ input: [ "file1.txt", "file2.txt" ] ]
    def processed4 = overrideIO(asMap, input4, []).arguments
    assert processed4.findAll{ it.name == "input" }[0].value == input4.input.join(":")

    // The output is a hash, first a single output
    def output1 = [ output: "file1.txt" ]
    def processed5 = overrideIO(asMap, [], output1).arguments
    assert processed5.findAll{ it.name == "output" }[0].value == output1.output

    // The output is a hash, first a single output
    def output2 = [ output: "file2.txt", log: "mylogfile.txt" ]
    def processed6 = overrideIO(asMap, [], output2).arguments
    assert ( processed6.findAll{ it.name == "output" }[0].value == output2.output
        && processed6.findAll{ it.name == "log" }[0].value == output2.log )

}

workflow outFromInTest {

    def sample = "sample1"

    def result = outFromIn(argumentsAsList(params.testing), sample)

    // No default value for the --output option is provided, use output as extension
    assert result.findAll{ it.name == "output" }[0].value == "sample1.testing.output"

    // A default value is provided for --log, use its extension
    assert result.findAll{ it.name == "log" }[0].value == "sample1.testing.txt"

}

workflow types {

    println("params:                   " + params.getClass() )
    println("params.testing:           " + params.testing.getClass() )
    println("params.testing.arguments: " + argumentsAsList(params.testing).getClass() )

    println("asList(params.testing):           " + argumentsAsList(params.testing).getClass() )
    println("asList(params.testing).arguments: " + argumentsAsList(params.testing).arguments.getClass() )

}

workflow joinOutput {

    def a = outFromIn(argumentsAsList(params.testing), "test123")
    def b = [ "outputfileinworkdir.output", "logfileinworkdir.txt" ]

    def aMap =
        a
            .findAll{ it.type == "file" && it.direction == "Output" }
            .collectEntries{ [(it.name): it.value] }
            .eachWithIndex{ m, i ->
                [(m.key), b[i]]
            }

    println(aMap)
    println(b)

}
