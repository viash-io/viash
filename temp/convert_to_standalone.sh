#!/bin/bash

# Store pre-hook info in temporary file
cat > pre-hook.sh <<- EOM
{{#env.data}}
wget {{uri}}
{{/env.data}}
EOM

# Store command in temporary file
cat > command.sh <<- EOM
docker run -i {{#env.contexts.docker.volumes}}-v "{{from}}:{{to}}" {{/env.contexts.docker.volumes}} \
   -w "{{env.contexts.docker.workdir}}" \
   {{env.contexts.docker.image}}
EOM

yq n function.command "`cat command.sh`" > command.yaml
yq n function.pre-hook "`cat pre-hook.sh`" > pre-hook.yaml
yq n extra "`cat {{function.env.code.file}}`" > extra.yaml

yq m -x test.yaml command.yaml > tmp1.yaml
yq m -x tmp1.yaml pre-hook.yaml > tmp2.yaml
yq m -x tmp2.yaml extra.yaml
