red=`tput setaf 1`
green=`tput setaf 2`
blue=`tput setaf 4`
reset=`tput sgr0`

SCRIPT_DIR=$( cd ${0%/*} && pwd -P )
VIASH_DIR="$SCRIPT_DIR/../"

function run_tests {
  if [ -f build.sbt ]; then
    echo ">> ${green}Running tests"
    sbt test
  else
    echo "${red}build.sbt not found. Are you in the right directory?${reset}"
  fi
}

function assembly_without_testing {
  if [ -f build.sbt ]; then
    echo ">> ${green}Building jar"
    sbt 'set test in assembly := {}' assembly
  else
    echo "build.sbt not found. Are you in the right directory?"
  fi
}

function assembly {
  if [ -f build.sbt ]; then
    echo ">> ${green}Running tests and building jar"
    sbt assembly
  else
    echo "${red}build.sbt not found. Are you in the right directory?${reset}"
  fi
}

function viash {
  java -jar "$VIASH_DIR/target/scala-2.12/viash-assembly-0.0.1.jar" $@
}

function viash_export_all {
  atom_dir="$1"
  output_dir="$2"

  platforms=("native" "docker" "nextflow")

  for a in `ls $atom_dir`; do
    echo ">> ${green}Processing atom $a${reset}"
    func_file="$atom_dir/$a/functionality.yaml"

    for p in ${platforms[@]}; do
      echo ">>>> ${blue}Processing platform $p${reset}"
      platform_file="$atom_dir/$a/platform_$p.yaml"

      if [ -f $platform_file ]; then
        # echo "      $func_file"
        # echo "      $platform_file"
        # echo "$JAVA -f $func_file -p $platform_file export -o output/${a}_${p}"
        viash -f $func_file -p $platform_file export -o "$output_dir/${a}_${p}"
      fi
    done
  done
}
