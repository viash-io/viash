FROM openjdk:8

# Install packages
RUN apt-get update && \
    apt-get install -y make git gzip

# Install sbt
RUN echo 'deb http://dl.bintray.com/sbt/debian /' > /etc/apt/sources.list.d/sbt.list && \
    apt-key adv --keyserver hkp://keyserver.ubuntu.com:80 --recv 99E82A75642AC823 && \
    apt-get -qq update && \
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

ENTRYPOINT [ "viash" ]
