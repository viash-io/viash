# see https://en.wikipedia.org/wiki/Syslog#Severity_level
VIASH_LOGCODE_EMERGENCY=0
VIASH_LOGCODE_ALERT=1
VIASH_LOGCODE_CRITICAL=2
VIASH_LOGCODE_ERROR=3
VIASH_LOGCODE_WARNING=4
VIASH_LOGCODE_NOTICE=5
VIASH_LOGCODE_INFO=6
VIASH_LOGCODE_DEBUG=7
VIASH_VERBOSITY=$VIASH_LOGCODE_NOTICE

# ViashLog: Log events depending on the verbosity level
# usage: ViashLog 1 alert Oh no something went wrong!
# $1: required verbosity level
# $2: display tag
# $3+: messages to display
# stdout: Your input, prepended by '[$2] '.
function ViashLog {
  local required_level="$1"
  local display_tag="$2"
  shift 2
  if [ $VIASH_VERBOSITY -ge $required_level ]; then
    echo "[$display_tag]" "$@"
  fi
}

# ViashEmergency: log events when the system is unstable
# usage: ViashEmergency Oh no something went wrong.
# stdout: Your input, prepended by '[emergency] '.
function ViashEmergency {
  ViashLog $VIASH_LOGCODE_EMERGENCY emergency "$@"
}

# ViashAlert: log events when actions must be taken immediately (e.g. corrupted system database)
# usage: ViashAlert Oh no something went wrong.
# stdout: Your input, prepended by '[alert] '.
function ViashAlert {
  ViashLog $VIASH_LOGCODE_ALERT alert "$@"
}

# ViashCritical: log events when a critical condition occurs
# usage: ViashCritical Oh no something went wrong.
# stdout: Your input, prepended by '[critical] '.
function ViashCritical {
  ViashLog $VIASH_LOGCODE_CRITICAL critical "$@"
}

# ViashError: log events when an error condition occurs
# usage: ViashError Oh no something went wrong.
# stdout: Your input, prepended by '[error] '.
function ViashError {
  ViashLog $VIASH_LOGCODE_ERROR error "$@"
}

# ViashWarning: log potentially abnormal events
# usage: ViashWarning Something may have gone wrong.
# stdout: Your input, prepended by '[warning] '.
function ViashWarning {
  ViashLog $VIASH_LOGCODE_WARNING warning "$@"
}

# ViashNotice: log significant but normal events
# usage: ViashNotice This just happened.
# stdout: Your input, prepended by '[notice] '.
function ViashNotice {
  ViashLog $VIASH_LOGCODE_NOTICE notice "$@"
}

# ViashInfo: log normal events
# usage: ViashInfo This just happened.
# stdout: Your input, prepended by '[info] '.
function ViashInfo {
  ViashLog $VIASH_LOGCODE_INFO info "$@"
}

# ViashDebug: log all events, for debugging purposes
# usage: ViashDebug This just happened.
# stdout: Your input, prepended by '[debug] '.
function ViashDebug {
  ViashLog $VIASH_LOGCODE_DEBUG debug "$@"
}
