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
  // Validate that every id emitted by the target channel is also present in
  // the source channel. This can only be checked reliably once both channels
  // have been fully drained: the two channels are processed independently
  // and concurrently, so checking membership while either channel is still
  // emitting is subject to a race condition, where a source id that hasn't
  // been observed yet is mistaken for a genuine mismatch.
  // See https://github.com/viash-io/viash/issues/605
  // Wrap each list of ids in a Map before combining. combine() concatenates
  // tuples when its inputs are Lists, which would otherwise flatten the two
  // id lists together instead of pairing them up as a single [sourceIds,
  // targetIds] event.
  def sourceIdsCh = sourceChannel | map{ tup -> tup[0].toString() } | toList() | map{ ids -> [ids: ids] }
  def targetIdsCh = targetChannel | map{ tup -> tup[0].toString() } | toList() | map{ ids -> [ids: ids] }

  sourceIdsCh.combine(targetIdsCh)
    | map{ sourceWrapped, targetWrapped ->
      def sourceIds = sourceWrapped.ids
      def targetIds = targetWrapped.ids
      def missingIds = (targetIds - sourceIds).unique()

      if (!missingIds.isEmpty()) {
        def id = missingIds[0]
        error (
          "Error in module '${key}' when merging output with original state.\n" +
          "  Reason: output with id '${id}' could not be joined with source channel.\n" +
          "    If the IDs in the output channel differ from the input channel,\n" +
          "    please set `tup[1]._meta.join_id to the original ID.\n" +
          "  Original IDs in input channel: ['${sourceIds.unique().join("', '")}'].\n" +
          "  Unexpected ID in the output channel: '${id}'.\n" +
          "  Example input event: [\"id\", [input: file(...)]],\n" +
          "  Example output event: [\"newid\", [output: file(...), _meta: [join_id: \"id\"]]]"
        )
      }
      // TODO: add link to our documentation on how to fix this
    }

  sourceChannel.cross(targetChannel)
    | map{ left, right -> right + left.drop(1) }
}
