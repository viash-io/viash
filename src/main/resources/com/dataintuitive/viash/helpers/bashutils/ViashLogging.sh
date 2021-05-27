VIASH_VERBOSITY=5

# see https://en.wikipedia.org/wiki/Syslog#Severity_level

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
  ViashLog 0 emergency $@
}

# ViashAlert: log events when actions must be taken immediately (e.g. corrupted system database)
# usage: ViashAlert Oh no something went wrong.
# stdout: Your input, prepended by '[alert] '. 
function ViashAlert {
  ViashLog 1 alert $@
}

# ViashCritical: log events when a critical condition occurs
# usage: ViashCritical Oh no something went wrong.
# stdout: Your input, prepended by '[critical] '. 
function ViashCritical {
  ViashLog 2 critical $@
}

# ViashError: log events when an error condition occurs
# usage: ViashError Oh no something went wrong.
# stdout: Your input, prepended by '[error] '. 
function ViashError {
  ViashLog 3 error $@
}

# ViashWarning: log potentially abnormal events
# usage: ViashWarning Something may have gone wrong.
# stdout: Your input, prepended by '[warning] '. 
function ViashWarning {
  ViashLog 4 warning $@
}

# ViashNotice: log significant but normal events
# usage: ViashNotice This just happened.
# stdout: Your input, prepended by '[notice] '. 
function ViashNotice {
  ViashLog 5 notice $@
}

# ViashInfo: log normal events
# usage: ViashInfo This just happened.
# stdout: Your input, prepended by '[info] '. 
function ViashInfo {
  ViashLog 6 info $@
}

# ViashDebug: log all events, for debugging purposes
# usage: ViashDebug This just happened.
# stdout: Your input, prepended by '[debug] '. 
function ViashDebug {
  ViashLog 7 debug $@
}
