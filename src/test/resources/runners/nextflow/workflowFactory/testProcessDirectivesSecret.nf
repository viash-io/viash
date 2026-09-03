
include { processDirectives } from params.workflowHelper

// The 'secret' directive accepts either a single String or a List[String]
// and should always be normalised to a List[String] of CharSequences.
// See src/main/resources/io/viash/runners/nextflow/workflowFactory/processDirectives.nf

// Case 1: a bare string is coerced into a single-element list
def result1 = processDirectives([secret: "MY_ACCESS_KEY"])
assert result1["secret"] == ["MY_ACCESS_KEY"]

// Case 2: a list of strings is passed through unchanged
def result2 = processDirectives([secret: ["MY_ACCESS_KEY", "MY_SECRET_KEY"]])
assert result2["secret"] == ["MY_ACCESS_KEY", "MY_SECRET_KEY"]

// Case 3: no 'secret' key at all is still valid (the directive is optional)
def result3 = processDirectives([:])
assert !result3.containsKey("secret")

// Calling an `include`-d function through Nextflow's DSL wraps any exception
// it throws in a java.lang.reflect.InvocationTargetException (the real
// message ends up on getCause(), not on the wrapper itself), so the failure
// cases below catch Throwable and inspect the root cause.

// Case 4: a non-string element in the list must be rejected
def rejectedNonString = false
try {
  processDirectives([secret: [123]])
} catch (Throwable e) {
  rejectedNonString = (e.getCause() ?: e) instanceof AssertionError
}
assert rejectedNonString : "processDirectives should reject non-String entries in 'secret'"

// Case 5: 'secret' must still be recognised as an expected key, i.e. it
// must not trip the "Unexpected keys in process directive" check that
// processDirectives runs first.
def rejectedUnexpectedKey = false
try {
  processDirectives([secret: "MY_ACCESS_KEY", bogusKey: "x"])
} catch (Throwable e) {
  def cause = e.getCause() ?: e
  rejectedUnexpectedKey = cause instanceof AssertionError && cause.getMessage().contains("bogusKey")
}
assert rejectedUnexpectedKey : "processDirectives should reject unexpected keys like 'bogusKey'"

println("testProcessDirectivesSecret.nf: all assertions passed")
