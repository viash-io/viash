#!/bin/bash

# VIASH START
par_namespace="cluster"
par_target="target"
# VIASH END

red=`tput setaf 1`
green=`tput setaf 2`
blue=`tput setaf 4`
reset=`tput sgr0`

for platform_path in `find $par_target/ -type d -depth 1`; do

  platform=`basename $platform_path`

  TARGET="$par_target/$platform/$par_namespace"
  README="$TARGET/README.md"

  # Cleanup
  if [ -f $README ]; then
    rm $README
  fi

  # TITLE
  echo "# ${par_namespace}"          >> $README
  echo ""                            >> $README

  # TOOLS
  echo "Tools in this namespace:"    >> $README
  echo ""                            >> $README
  for i in `find $TARGET -type d -depth 1` ; do
      tool_name=`basename $i`
      echo "- $tool_name"            >> $README
  done
  echo ""                            >> $README

  # USE
  echo "## Tools and Options"        >> $README
  echo ""                            >> $README
  for i in `find $TARGET -type d -depth 1` ; do
      tool_name=`basename $i`
      echo "## $tool_name"           >> $README
      echo ""                        >> $README
      echo '```'                     >> $README
      $i/$tool_name --help           >> $README
      echo '```'                     >> $README
      echo ""                        >> $README
  done

done
