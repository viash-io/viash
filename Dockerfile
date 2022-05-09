FROM openjdk:8

# Install packages
RUN apt-get update && \
    apt-get install -y make git gzip

# Install sbt
RUN echo "deb https://repo.scala-sbt.org/scalasbt/debian /" | tee -a /etc/apt/sources.list.d/sbt.list && \
    curl -sL "https://keyserver.ubuntu.com/pks/lookup?op=get&search=0x2EE0EA64E40A89B84B2DF73499E82A75642AC823" | apt-key add && \
    apt-get update && \
    apt-get install -y sbt

# Install yq
RUN curl -sSL https://github.com/mikefarah/yq/releases/download/v4.6.1/yq_linux_386 > /usr/bin/yq && \
    chmod +x /usr/bin/yq

# Run SBT once so that all libraries are downloaded
# Avoid running sbt from /
WORKDIR /app
RUN sbt exit

# Get sources
WORKDIR /app
COPY . /app/viash/
WORKDIR /app/viash

# Build, package, install
RUN ./configure
RUN make bin/viash
RUN make install
RUN make tools
