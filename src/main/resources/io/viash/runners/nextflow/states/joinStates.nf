def joinStates(Closure apply_) {
  workflow joinStatesWf {
    take: input_ch
    main:
    output_ch = input_ch
      | toSortedList
      | filter{ it.size() > 0 }
      | map{ tups ->
        def ids = tups.collect{it[0]}
        def states = tups.collect{it[1]}
        apply_(ids, states)
      }

    emit: output_ch
  }
  return joinStatesWf
}