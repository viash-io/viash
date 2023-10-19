workflow base {
  take: input_ch
  main:

  comps = [
    "a", "b", "c", "d", "e", "f", "g", "h", "i", "j", "k", "l", "m",
    "n", "o", "p", "q", "r", "s", "t", "u", "v", "w", "x", "y", "z"
  ]
    .collect{ let ->
      step1.run(key: "step1" + let)
    }

  // generate list from 0 to 1000
  ch = Channel.fromList(0..100)
  
    | map { num ->
      // create temporary file
      file = tempFile()
      file.write("num: $num")

      ["num$num", [ num: num, file: file ]]
    }

    | step1.run(
      fromState: ["input": "file"],
      toState: ["step1_output": "output"]
    )


    | step1.run(
      key: "step1bis",
      fromState: { id, state ->
        ["input": state.step1_output]
      },
      toState: { id, output, state ->
        state + ["step1bis_output": output.output]
      }
    )

    | runEach(
      components: comps,
      filter: { id, state, comp ->
        id != "num0" || comp.name == "step1a"
      },
      id: { id, state, comp ->
        "${id}_${comp.name}".toString()
      },
      fromState: { id, state, comp ->
        ["input": state.step1bis_output]
      },
      toState: { id, output, state, comp ->
        state + [
          "runEach_output": output.output,
          "runEach_key": comp.name,
          "extra_key": "foo"
        ]
      }
      // todo: test filter
      // todo: test fromState and toState as maps or arrays
    )

    | view { id, state ->
      def num = state.num

      // check id
      assert id ==~ /num${num}_step1[a-z]/: "id should match 'num${num}_step1[a-z]'. Found: '$id'"

      // check file text
      def file_text = state.file.toFile().readLines()[0]
      assert file_text == "num: $num": "file text should be 'num: $num'. Found: '$file_text'"

      // check runEach_output text
      def runEach_output_text = state.runEach_output.toFile().readLines()[0]
      assert runEach_output_text == "num: $num": "runEach_output text should be 'num: $num'. Found: '$runEach_output_text'"

      // check extra_key
      assert state.extra_key == "foo": "extra_key should be 'foo'. Found: '${state.extra_key}'"

      // check runEach_key
      assert state.runEach_key ==~ /step1[a-z]/: "runEach_key should match 'step1[a-z]'. Found: '${state.runEach_key}'"

      if (num == 0) {
        "after runEach: id: $id, state: $state"
      } else {
        null
      }
    }

    | toSortedList{ a, b -> a[0] <=> b[0] }
    | view { list ->
      assert list.size() == 100 * 25 + 1: "list size should be 100 * 25 + 1. Found: ${list.size()}"

      def ids = list.collect{it[0]}
      def expectedIds = (0..100).collectMany{ num ->
        if (num == 0) {
          ["num0_step1a"]
        } else {
          comps.collect{ comp ->
            "num${num}_${comp.name}".toString()
          }
        }
      }
      def unexpectedIds = ids - expectedIds
      def missingIds = expectedIds - ids

      // println()
      // println()
      // println("ids: $ids")
      // println()
      // println("expectedIds: $expectedIds")
      // println()
      // println("unexpectedIds: $unexpectedIds")
      // println()
      // println("missingIds: $missingIds")
      // println()
      // println()
      assert unexpectedIds.size() == 0: "unexpected ids: $unexpectedIds"
      assert missingIds.size() == 0: "missing ids: $missingIds"

      null
    }
  emit:
  input_ch
}
