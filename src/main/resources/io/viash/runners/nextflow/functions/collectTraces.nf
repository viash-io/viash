class CustomTraceObserver implements nextflow.trace.TraceObserver {
  List traces

  CustomTraceObserver(List traces) {
    this.traces = traces
  }

  @Override
  void onProcessComplete(nextflow.processor.TaskHandler handler, nextflow.trace.TraceRecord trace) {
    def trace2 = trace.store.clone()
    trace2.script = null
    traces.add(trace2)
  }

  @Override
  void onProcessCached(nextflow.processor.TaskHandler handler, nextflow.trace.TraceRecord trace) {
    def trace2 = trace.store.clone()
    trace2.script = null
    traces.add(trace2)
  }
}

def collectTraces() {
  def traces = Collections.synchronizedList([])

  // add custom trace observer which stores traces in the traces object
  session.observers.add(new CustomTraceObserver(traces))

  traces
}
