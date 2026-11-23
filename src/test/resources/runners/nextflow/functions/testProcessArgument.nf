
include { _processArgument } from params.workflowHelper

// each test case: [description, input argument, expected default]
def testCases = [
  [
    "output file without must_exist/required gets a fallback default (must_exist defaults to true)",
    [name: "--output", type: "file", direction: "output", example: "foo.txt"],
    '$id.$key.output.txt'
  ],
  [
    "output file with must_exist: true gets a fallback default",
    [name: "--output", type: "file", direction: "output", must_exist: true, required: false, example: "foo.txt"],
    '$id.$key.output.txt'
  ],
  [
    "required output file with must_exist: false gets a fallback default",
    [name: "--output", type: "file", direction: "output", must_exist: false, required: true, example: "foo.txt"],
    '$id.$key.output.txt'
  ],
  [
    "optional output file with must_exist: false does not get a fallback default",
    [name: "--output", type: "file", direction: "output", must_exist: false, required: false, example: "foo.txt"],
    null
  ],
  [
    "optional output file with must_exist: false and required unset does not get a fallback default",
    [name: "--output", type: "file", direction: "output", must_exist: false, example: "foo.txt"],
    null
  ],
  [
    "multiple output file gets a fallback default list",
    [name: "--output", type: "file", direction: "output", multiple: true, example: ["foo.txt"]],
    ['$id.$key.output_*.txt']
  ],
  [
    "optional multiple output file with must_exist: false does not get a fallback default",
    [name: "--output", type: "file", direction: "output", multiple: true, must_exist: false, required: false, example: ["foo.txt"]],
    null
  ],
  [
    "explicit default of an optional output file with must_exist: false is kept",
    [name: "--output", type: "file", direction: "output", must_exist: false, required: false, default: "bar.txt"],
    "bar.txt"
  ],
  [
    "input file does not get a fallback default",
    [name: "--input", type: "file", direction: "input", example: "foo.txt"],
    null
  ]
]

testCases.each { description, arg, expectedDefault ->
  def result = _processArgument(arg)
  println("${description}: default = ${result.default}")
  assert result.default == expectedDefault : "${description}: expected default '${expectedDefault}', got '${result.default}'"
}
