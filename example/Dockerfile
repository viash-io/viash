FROM rocker/tidyverse:3.6.2

# install common packages and dynwrap/dyncli
RUN echo 'options(repos = c(CRAN = "https://cran.rstudio.com/"));utils::setRepositories(ind=1:4)' > ~/.Rprofile
RUN R -e 'install.packages(c("devtools", "remotes", "optparse"))' && \
    rm -rf /tmp/* # clean up temp files

RUN mkdir /home/app

# Install dependencies: fwatchdog, yq and porta.sh ... and gawk
ENV PORTASH_VERSION=v0.1.0
RUN apt-get install -y curl bash gawk \
    && echo "Pulling watchdog binary from Github." \
    && curl -sSL https://github.com/openfaas/faas/releases/download/0.18.0/fwatchdog > /usr/bin/fwatchdog \
    && chmod +x /usr/bin/fwatchdog \
    && cp /usr/bin/fwatchdog /home/app \
    && echo "Pulling porta.sh from Github." \
    && curl -sSL https://github.com/data-intuitive/Portash/releases/download/$PORTASH_VERSION/porta.sh > /usr/bin/porta.sh \
    && chmod +x /usr/bin/porta.sh \
    && echo "Pulling yq from Github." \
    && curl -sSL https://github.com/mikefarah/yq/releases/download/2.4.0/yq_linux_386 > /usr/bin/yq \
    && chmod +x /usr/bin/yq \
    && rm -rf /var/lib/apt/lists/*

# fwatchdog config
EXPOSE 8080
ENV fprocess="porta.sh"
ENV write_debug="true"
ENV read_timeout=3600
ENV write_timeout=3600

HEALTHCHECK --interval=3s CMD [ -e /tmp/.lock ] || exit 1
CMD [ "fwatchdog" ]
