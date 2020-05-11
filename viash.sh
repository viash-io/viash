#!/bin/sh

source temp/helper.sh

echo "${green}"
echo "██╗   ██╗██╗ █████╗ ███████╗██╗  ██╗"
echo "██║   ██║██║██╔══██╗██╔════╝██║  ██║"
echo "██║   ██║██║███████║███████╗███████║"
echo "╚██╗ ██╔╝██║██╔══██║╚════██║██╔══██║"
echo " ╚████╔╝ ██║██║  ██║███████║██║  ██║"
echo "  ╚═══╝  ╚═╝╚═╝  ╚═╝╚══════╝╚═╝  ╚═╝"
echo "${reset}"

echo ">> ${green}Building the assembly JAR using Docker...${reset}"

# Build the container
docker build -t viash .
# Create a writable overlay
id=$(docker create -it --name viash-cp viash bash)
# Extract the assembly JAR
docker cp viash-cp:"/app/viash/target/scala-2.12/viash-assembly-$VERSION.jar" atoms/viash/
# Remove the image
docker rm -f viash-cp

echo ">> ${green}Installing Viash in ~/bin, make sure it's in your \$PATH...${reset}"

if [ -d ~/bin ]; then
  java -jar "$VIASH_DIR/atoms/viash/viash-assembly-$VERSION.jar" \
    export \
    --functionality "$VIASH_DIR/atoms/viash/functionality.yaml" \
    --platform "$VIASH_DIR/atoms/viash/platform_native.yaml" \
    --output ~/bin
else
  echo "OEPS, ~/bin directory does not exist!"
fi

echo ">> ${green}Done${reset}"
