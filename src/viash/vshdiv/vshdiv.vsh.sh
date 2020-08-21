#' functionality:
#'   name: vshdiv
#'   version: 1.0
#'   description: |
#'     Split files from the new format into separate files from the old format.
#'   arguments:
#'   - name: "script"
#'     type: file
#'     description: A vsh script.
#'     must_exist: true
#'   - name: "--rm"
#'     type: boolean_true
#'     description: Remove the source files after use.
#' platforms:
#' - type: docker
#'   image: mikefarah/yq
#'   apk: 
#'     packages:
#'     - bash

set -e

#### MAKE VARIABLES
fun_path=`dirname "$par_script"`/functionality.yaml
script_path=`echo "$par_script" | sed 's#\.vsh##'`
script_ext=`echo "$script_path" | sed 's#.*\.##'`

if [ "$script_ext" = "sh" ]; then
  script_type="bash"
elif [ "$script_ext" = "R" ] || [ "$script_ext" = "r" ]; then
  script_type="r"
elif [ "$script_ext" = "py" ]; then
  script_type="python"
else
  echo "Unsupported format: $script_ext!"
  exit 1
fi

echo "Splitting [`basename $par_script`] into multiple files!"

#### FUNCTIONALITY
echo "> Deriving [`basename $fun_path`] from [`basename $par_script`]"
cat "$par_script" | grep "^#' " | sed "s/^#' //" | yq read - functionality | yq d - resources > "$fun_path"

# add script as resource
printf "resources:\n- type: ${script_type}_script\n  path: `basename "$script_path"`\n" | yq m "$fun_path" - -i

has_resources=`cat "$par_script" | grep "^#' " | sed "s/^#' //" | yq read - functionality.resources | head -1`
if [ ! -z "$has_resources" ]; then
  echo adding more resources
  cat "$par_script" | grep "^#' " | sed "s/^#' //" | yq read - functionality.resources | yq p - resources | yq m -a "$fun_path" - -i
fi

#### PLATFORM(S)
# create platform yamls
platforms=`cat "$par_script" | grep "^#' " | sed "s/^#' //" | yq read - platforms.*.type`
for plat in $platforms; do
  platform_path="`dirname $par_script`/platform_${plat}.yaml"
  echo "> Deriving [`basename $platform_path`] from [`basename $par_script`]"
  cat "$par_script" | grep "^#' " | sed "s/^#' //" | yq read - platforms.[type==$plat] > "$platform_path"
done

#### SCRIPT
  echo "> Deriving [`basename $script_path`] from [`basename $par_script`]"
if [ "$script_ext" = "sh" ] || [ "$script_ext" = "R" ] || [ "$script_ext" = "r" ] || [ "$script_ext" = "py" ]; then 
cat > "$script_path" << HERE
## VIASH$my_viash_dummy START

## VIASH$my_viash_dummy END
HERE
fi

cat "$par_script" | grep -v "^#' " >> "$script_path"

#### CLEANUP
if [ "$par_rm" = "true" ]; then
  echo -n "> Removing source files: [$par_script]"
  rm "$par_script"
fi
