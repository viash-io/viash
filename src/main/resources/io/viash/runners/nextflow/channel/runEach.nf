/**
 * Run a list of components on a stream of data.
 * 
 * @param components: list of Viash VDSL3 modules to run
 * @param fromState: a closure, a map or a list of keys to extract from the input data.
 *   If a closure, it will be called with the id, the data and the component itself.
 * @param toState: a closure, a map or a list of keys to extract from the output data
 *   If a closure, it will be called with the id, the output data, the old state and the component itself.
 * @param filter: filter function to apply to the input.
 *   It will be called with the id, the data and the component itself.
 * @param id: id to use for the output data
 *   If a closure, it will be called with the id, the data and the component itself.
 * @param auto: auto options to pass to the components
 *
 * @return: a workflow that runs the components
 **/
def runEach(Map args) {
  assert args.components: "runEach should be passed a list of components to run"

  def components_ = args.components
  if (components_ !instanceof List) {
    components_ = [ components_ ]
  }
  assert components_.size() > 0: "pass at least one component to runEach"

  def fromState_ = args.fromState
  def toState_ = args.toState
  def filter_ = args.filter
  def id_ = args.id

  workflow runEachWf {
    take: input_ch
    main:

    // generate one channel per method
    out_chs = components_.collect{ comp_ ->
      filter_ch = filter_
        ? input_ch | filter{tup ->
          filter_(tup[0], tup[1], comp_)
        }
        : input_ch
      id_ch = id_
        ? filter_ch | map{tup ->
          // def new_id = id_(tup[0], tup[1], comp_)
          def new_id = tup[0]
          if (id_ instanceof String) {
            new_id = id_
          } else if (id_ instanceof Closure) {
            new_id = id_(new_id, tup[1], comp_)
          }
          [new_id] + tup.drop(1)
        }
        : filter_ch
      data_ch = id_ch | map{tup ->
          def new_data = tup[1]
          if (fromState_ instanceof Map) {
            new_data = fromState_.collectEntries{ key0, key1 ->
              [key0, new_data[key1]]
            }
          } else if (fromState_ instanceof List) {
            new_data = fromState_.collectEntries{ key ->
              [key, new_data[key]]
            }
          } else if (fromState_ instanceof Closure) {
            new_data = fromState_(tup[0], new_data, comp_)
          }
          tup.take(1) + [new_data] + tup.drop(1)
        }
      out_ch = data_ch
        | comp_.run(
          auto: (args.auto ?: [:]) + [simplifyInput: false, simplifyOutput: false]
        )
      post_ch = toState_
        ? out_ch | map{tup ->
          def output = tup[1]
          def old_state = tup[2]
          if (toState_ instanceof Map) {
            new_state = old_state + toState_.collectEntries{ key0, key1 ->
              [key0, output[key1]]
            }
          } else if (toState_ instanceof List) {
            new_state = old_state + toState_.collectEntries{ key ->
              [key, output[key]]
            }
          } else if (toState_ instanceof Closure) {
            new_state = toState_(tup[0], output, old_state, comp_)
          }
          [tup[0], new_state] + tup.drop(3)
        }
        : out_ch
      
      post_ch
    }

    // mix all results
    output_ch =
      (out_chs.size == 1)
        ? out_chs[0]
        : out_chs[0].mix(*out_chs.drop(1))

    emit: output_ch
  }

  return runEachWf
}
