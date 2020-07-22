README
================

  - [First execution](#first-execution)
  - [Overview](#overview)

Viash helps you build software components from small R/Python/Bash
scripts. By providing some meta-data regarding the functionality
provided by the component and the platform on which you want to run the
software, viash can help you:

  - wrap your script in an executable with a CLI and –help functionality
  - execute it natively or in a Docker container
  - combine multiple components in a Nextflow pipeline
  - unit-test your component to ensure that it works at all times

We are currently working on increasing reproducability with viash by
tying the executable to a particular Docker container.

## First execution

``` sh
viash run -f ...
```

## Overview

  - Viash sub-commands
  - Wrapping a Bash script
  - Wrapping an R script
  - Wrapping a Python script
  - Wrapping an R Markdown report
  - Description of the functionality.yaml format
  - Description of the platform.yaml format
