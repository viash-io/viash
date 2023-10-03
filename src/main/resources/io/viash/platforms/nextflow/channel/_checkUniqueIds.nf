
/**
 * Check if the ids are unique across parameter sets
 *
 * @param parameterSets a list of parameter sets.
 */
private void _checkUniqueIds(List<Tuple2<String, Map<String, Object>>> parameterSets) {
  def ppIds = parameterSets.collect{it[0]}
  assert ppIds.size() == ppIds.unique().size() : "All argument sets should have unique ids. Detected ids: $ppIds"
}
