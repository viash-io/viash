#' functionality:
#'   name: version
#'   description: Version all functionality and platform YAML files recursively
#'   arguments:
#'     - name: "version"
#'       type: string
#'       required: true
#'     - name: "--location"
#'       type: file
#'       alternatives: ["-l"]
#'       default: "src"
#'   resources: []
#'   version: 0.2.0-rc2
#' platforms:
#' - type: native
#'   version: 0.2.0-rc2
#' - type: docker
#'   image: dataintuitive/viash
#'   workdir: /pwd
#'   version: 0.2.0-rc2
#!/bin/bash


for i in `find "$par_location" -name functionality.yaml`; do
  echo "Inserting/replacing version in $i"
  yq w -i --tag '!!str' "$i" version "$par_version"
done

for i in `find "$par_location" -name platform*.yaml`; do
  echo "Inserting/replacing version in $i"
  yq w -i --tag '!!str' "$i" version "$par_version"
done
