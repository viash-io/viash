# ViashAbsolutePath: generate absolute path from relative path
# borrowed from https://stackoverflow.com/a/21951256
# $1     : relative filename
# return : absolute path
# examples:
#   ViashAbsolutePath some_file.txt   # returns /path/to/some_file.txt
#   ViashAbsolutePath /foo/bar/..     # returns /foo
function ViashAbsolutePath {
  local thePath
  local parr
  local outp
  local len
  if [[ ! "$1" =~ ^/ ]]; then
    thePath="$PWD/$1"
  else
    thePath="$1"
  fi
  echo "$thePath" | (
    IFS=/
    read -a parr
    declare -a outp
    for i in "${parr[@]}"; do
      case "$i" in
      ''|.) continue ;;
      ..)
        len=${#outp[@]}
        if ((len==0)); then
          continue
        else
          unset outp[$((len-1))]
        fi
        ;;
      *)
        len=${#outp[@]}
        outp[$len]="$i"
      ;;
      esac
    done
    echo /"${outp[*]}"
  )
}
