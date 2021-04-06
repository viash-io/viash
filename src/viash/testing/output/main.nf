nextflow.preview.dsl=2
import java.nio.file.Paths

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

// Use the params map, create a hashmap of the filenames for output
// output filename is <sample>.<method>.<arg_name>[.extension]
def outFromIn(_params, sample) {

   _params
        .arguments
        .findAll{ it -> it.type == "file" && it.direction == "Output" }
        .collect{ it ->
            // If a default (dflt) attribute is present, strip the extension from the filename,
            // otherwise just use the option name as an extension.
            def extOrName = (it.dflt != null) ? it.dflt.split(/\./).last() : it.name
            // The output filename is <sample> . <modulename> . <extension>
            def defaultName = sample + "." + "testing" + "." + extOrName
            it + [ value : defaultName ]
        }

}

// Convert the nextflow.config arguments list to a simple Map instead of a LinkedMap
// The rest of this main.nf script uses the Map form
def toMap(_params) {
    def overrideArgs = _params.arguments.collect{ key, value -> value }
    def newParams = _params + [ "arguments" : overrideArgs ]
    return newParams
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

/* def overrideOutput(params, str) { */

/*     def update = [ "value" : str ] */

/*     def overrideArgs = params.arguments.collect{it -> */
/*       (it.direction == "Output" && it.type == "file") */
/*         ? it + update */
/*         : it */
/*     } */

/*     def newParams = params + [ "arguments" : overrideArgs ] */

/*     return newParams */
/* } */


process testing_process {
  
  tag "${id}"
  echo { (params.debug == true) ? true : false }
  cache 'deep'
  stageInMode "symlink"
  container "${container}"
  
  input:
    tuple val(id), path(input), val(output), val(container), val(cli)
  output:
    tuple val("${id}"), path("${output}"), path("log.txt")
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
            // TODO: make sure input is List[Path], HashMap[String,Path] or Path, otherwise convert
            // NXF knows how to deal with an List[Path], not with HashMap !
            def checkedInput =
                (input in HashMap)
                    ? input.collect{ k, v -> v }.flatten()
                    : input
            // filename is either String, List[String] or HashMap[String, String]
            def inputs =
                (input in List || input in HashMap)
                    ? (input in List)
                        ? input.collect{ it.name }
                        : input.collectEntries{ k, v -> [ k, (v in List) ? v.collect{it.name} : v.name ] }
                    : input.name
            def defaultParams = params[key] ? params[key] : [:]
            def overrideParams = _params[key] ? _params[key] : [:]
            def updtParams = defaultParams + overrideParams
            // now, switch to arrays instead of hashes...
            /* def outputFilename = (!params.test) ? outFromIn(filename) : updtParams.output */
            def outputs = (!params.test) ? outFromIn(filename) : updtParams.output
            def updtParams1 = overrideIO(updtParams, inputs, outputs)
            /* def updtParams2 = overrideOutput(updtParams1, outputFilename) */
            new Tuple5(
                id,
                checkedInput,
                outputFilename,
                effectiveContainer(updtParams2),
                renderCLI([updtParams2.command], updtParams2.arguments)
            )
        }
    result_ = testing_process(id_input_output_function_cli_) \
        | join(id_input_params_) \
        | map{ id, output, input, original_params ->
            new Tuple3(id, output, original_params)
        }

    emit:
    result_

}

workflow {

   def id = params.id
   def ch_ = Channel.fromPath(params.input).map{ s -> new Tuple3(id, s, params)}

   testing(ch_) \
        | view{ [ it[0], it[1] ] }
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

    def asMap = toMap(params.testing)

    def base = overrideIO(asMap, [], [])

    def input1 = "fileabcd.txt"
    def processed1 = overrideIO(asMap, input1, []).arguments
    def test1 = processed1.findAll{ it.name == "input" }[0].value == input1
    println( "Test 1: $test1")

    // Passing an array means multiple options
    def input2 = [ "file1.txt", "file2.txt" ]
    def processed2 = overrideIO(asMap, input2, []).arguments
    def test2 = processed2.findAll{ it.name == "input" }[0].value == input2.join(":")
    println( "Test 2: $test2" )

    // The input2 key does not occur in the arguments list, so is omitted
    def input3 = [ input: "file1.txt", input2: "file2.txt" ]
    def processed3 = overrideIO(asMap, input3, []).arguments
    def test3 = processed3.findAll{ it.name == "input" }[0].value == input3.input
    println( "Test 3: $test3")

    // The input key is an array
    def input4 = [ input: [ "file1.txt", "file2.txt" ] ]
    def processed4 = overrideIO(asMap, input4, []).arguments
    def test4 = processed4.findAll{ it.name == "input" }[0].value == input4.input.join(":")
    println( "Test 4: $test4")

    // The output is a hash, first a single output
    def output1 = [ output: "file1.txt" ]
    def processed5 = overrideIO(asMap, [], output1).arguments
    def test5 = processed5.findAll{ it.name == "output" }[0].value == output1.output
    println( "Test 5: $test5")

    // The output is a hash, first a single output
    def output2 = [ output: "file2.txt", log: "mylogfile.txt" ]
    def processed6 = overrideIO(asMap, [], output2).arguments
    def test6 = ( processed6.findAll{ it.name == "output" }[0].value == output2.output
        && processed6.findAll{ it.name == "log" }[0].value == output2.log )
    println( "Test 6: $test6")

}

workflow outFromInTest {

    def sample = "sample1"

    def result = outFromIn(toMap(params.testing), sample)

    def test1 = result.findAll{ it.name == "output" }[0].value == "sample1.testing.output"
    println( "Test 1: $test1")

    def test2 = result.findAll{ it.name == "log" }[0].value == "sample1.testing.txt"
    println( "Test 2: $test2")

}
