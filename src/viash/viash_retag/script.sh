#!/bin/bash
shopt -s extglob

# For older docker versions, docker manifest inspect is an experimental feature,
# which requires DOCKER_CLI_EXPERIMENTAL=enabled
# Docker manifest inspect --help should always exit with status code 0 if the command is found,
# so we must check the output itself and not the exit code.
docker_exists=$(docker manifest inspect --help > /dev/null 2> /dev/null)
if ! $docker_exits; then
    echo "Failed to run docker manifest inspect --help."
    exit 1
fi

docker_test=$(docker manifest inspect --help 2> /dev/null)
if [ "$docker_test" == "docker manifest inspect is only supported on a Docker cli with experimental cli features enabled" ]; then
    echo "Older version of docker detected, enabling experimental docker cli features."
    export DOCKER_CLI_EXPERIMENTAL=enabled
fi

# Check if build_dir is a directory, not a file (viash checks if the path exists)
if [[ ! -d "$par_build_dir" ]]; then
    echo "Viash build output directory '$par_build_dir' is not a directory, exiting..." 
    exit 1
fi

# Remove trailing slashes (requires extglob)
par_target_registry="${par_target_registry%%+(/)}"
par_source_registry="${par_source_registry%%+(/)}"
par_organization="${par_organization%%+(/)}"
# Check if some parameters are not empty
if [ -z "$par_target_registry" ]; then
    echo "Target docker registry must not be an empty string."
    exit 1
fi

if [ -z "$par_source_registry" ]; then
    echo "Source docker registry must not be an empty string."
    exit 1
fi

if [ -z "$par_tag" ]; then
    echo "Tag must not be an empty string."
    exit 1
fi

if [ -z "$par_organization" ]; then
    echo "Organization must not be an empty string."
    exit 1
fi

# List all folders in the "target/docker"
TARGET_FOLDER="$par_build_dir/docker"
if [[ ! -d "$TARGET_FOLDER" && -L "$TARGET_FOLDER" ]]; then
    echo "$TARGET_FOLDER does not exits, please build the docker containers first."
    exit 1;
fi

# Adopted from https://stackoverflow.com/questions/23356779/how-can-i-store-the-find-command-results-as-an-array-in-bash
# Alternative for mapfile for older bash versions.
echo "Looking for components in $TARGET_FOLDER"
components=()
tmpfile=$(mktemp)
find "$TARGET_FOLDER" -maxdepth 2 -mindepth 2 -type d -print0 > "$tmpfile"
while IFS=  read -r -d $'\0'; do
    component_name=${REPLY#"$TARGET_FOLDER/"}
    component_name_underscore=${component_name//\//_}
    components+=("$par_source_registry/$par_organization/$component_name_underscore:$par_tag")
done <"$tmpfile"
rm -f tmpfile

if [ ${#components[@]} -eq 0 ]; then
    echo "No components found in $TARGET_FOLDER, exiting!" 
    exit 1
fi

for i in "${components[@]}"
do
    printf "\t$i\n"
done

# Check if all images exist before pulling
echo "Checking if all component docker images can be found at $par_source_registry/$par_organization."
function check_image_exists {
  docker manifest inspect "$1" > /dev/null 2> /dev/null
}
for i in "${components[@]}"
do
    check_image_exists "$i"
    exit_code=$?
    if [ $exit_code -eq 1 ]; then
        echo "Image with id $i not found. Either you do not have enough permissions" \
             "to access the repository or the image does not exist." \
             "Tip: check the output from 'docker manifest inspect $i'. Exiting..."
        exit 1
    fi
done

# Actually pull the images
echo "Pulling images."
for i in "${components[@]}"
do
    if [ "$par_dry_run" = "false" ]; then
        docker pull "$i" || {
            printf "Failed to pull image $i, exiting!"; 
            exit 1
        }
    else
        printf "\tDry run enabled, would try to pull $i\n"
    fi
done

# Re-tag docker containers
echo "Tagging docker containers" 
for i in "${components[@]}"
do
    if [ "$par_dry_run" = "false" ]; then
        docker tag "$i" "${i//$par_source_registry/$par_target_registry}" || {
            echo "Failed to tag $i as ${i//$par_source_registry/$par_target_registry}, exiting...";
            exit 1
        }
    else
        printf "\tDry run enabled, would have renamed $i to ${i//$par_source_registry/$par_target_registry}\n"
    fi
done

echo "Finished!"
echo "Hint: Images can be pushed to the new repository using 'viash_push --registry $par_target_registry --mode release --tag $par_tag'"