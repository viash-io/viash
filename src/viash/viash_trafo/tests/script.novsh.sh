#' functionality:
#'   name: hello
#'   arguments:
#'     - name: "name"
#'       type: string
#'       default: Bob
#'   resources:
#'     - type: bash_script
#'       path: script.sh
#' platforms:
#'   - type: native
#'   - type: docker
#'     image: bash:4.0

echo Hello $par_name
