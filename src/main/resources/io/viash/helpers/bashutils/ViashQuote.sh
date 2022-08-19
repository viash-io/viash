# ViashQuote: put quotes around non flag values
# $1     : unquoted string
# return : possibly quoted string
# examples:
#   ViashQuote --foo      # returns --foo
#   ViashQuote bar        # returns 'bar'
#   Viashquote --foo=bar  # returns --foo='bar'
function ViashQuote {
  if [[ "$1" =~ ^-+[a-zA-Z0-9_\-]+=.+$ ]]; then
    echo "$1" | sed "s#=\(.*\)#='\1'#"
  elif [[ "$1" =~ ^-+[a-zA-Z0-9_\-]+$ ]]; then
    echo "$1"
  else
    echo "'$1'"
  fi
}
