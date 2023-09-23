def setState(fun) {
  workflow setStateWf {
    take: input_ch
    main:
      output_ch = input_ch
        | map { tup ->
          def id = tup[0]
          def state = tup[1]
          def unfilteredState = fun(id, state)
          def newState = unfilteredState.findAll{key, val -> val != null}
          [id, newState] + tup.drop(2)
        }
    emit: output_ch
  }
  return setStateWf
}
