# ViashRemoveFlags: Remove leading flag
# $1     : string with a possible leading flag
# return : string without possible leading flag
# examples:
#   ViashRemoveFlags --foo=bar  # returns bar
function ViashRemoveFlags {
  echo "$1" | sed 's/^--*[a-zA-Z0-9_\-]*=//'
}
