# ViashExtractFlags: Retain leading flag
# $1     : string with a possible leading flag
# return : leading flag
# examples:
#   ViashExtractFlags --foo=bar  # returns --foo
function ViashExtractFlags {
  echo $1 | sed 's/=.*//'
}
