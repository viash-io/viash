/**
 * Join sourceChannel to targetChannel
 * 
 * This function joins the sourceChannel to the targetChannel. 
 * However, each id in the targetChannel must be present in the
 * sourceChannel. If _meta.join_id exists in the targetChannel, that is 
 * used as an id instead. If the id doesn't match any id in the sourceChannel,
 * an error is thrown.
 */

def safeJoin(targetChannel, sourceChannel, key) {
  def sourceIDs = new IDChecker()

  def sourceCheck = sourceChannel
    | map { tup ->
      sourceIDs.observe(tup[0])
      tup
    }
  def targetCheck = targetChannel
    | map { tup ->
      def id = tup[1].get("_meta")?.get("join_id") ?: tup[0]
      
      if (!sourceIDs.contains(id)) {
        error (
          "Error in module '${key}' when merging output with original state.\n" +
          "  Reason: output with id '${id}' could not be joined with source channel.\n" +
          "    If the IDs in the output channel differ from the input channel,\n" + 
          "    please set `tup[1]._meta.join_id to the original ID.\n" +
          "  Original IDs in input channel: ['${sourceIDs.getItems().join("', '")}'].\n" + 
          "  Unexpected ID in the output channel: '${id}'.\n" +
          "  Example input event: [\"id\", [input: file(...)]],\n" +
          "  Example output event: [\"newid\", [output: file(...), _meta: [join_id: \"id\"]]]"
        )
      }
      // TODO: add link to our documentation on how to fix this

      tup
    }
  targetCheck.join(sourceCheck)
}
