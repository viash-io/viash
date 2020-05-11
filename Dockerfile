FROM centos:8

# IMPORT the Centos-7 GPG key to prevent warnings
RUN rpm --import http://mirror.centos.org/centos/RPM-GPG-KEY-CentOS-7

# Add bintray repository where the SBT binaries are published
RUN curl -sS https://bintray.com/sbt/rpm/rpm | tee /etc/yum.repos.d/bintray-sbt-rpm.repo

# Base Install + JDK
RUN yum -y update && \
    yum -y install java-1.8.0-openjdk && \
    yum -y install java-1.8.0-openjdk-devel && \
    yum -y install sbt && \
    yum -y update bash && \
    rm -rf /var/cache/yum/* && \
    yum clean all

# Run SBT once so that all libraries are downloaded
RUN sbt exit

# Install some packages
RUN yum -y install git && \
    yum -y install gzip

WORKDIR /app

# get Repo
COPY . /app/viash/

WORKDIR /app/viash

# Build and package
RUN sbt 'set test in assembly := {}' assembly
# universal:packageBin stage

