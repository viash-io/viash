/*
 * Copyright (C) 2020  Data Intuitive
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package io.viash.runners.nextflow

import io.viash.helpers.data_structures._
import io.viash.schemas._

// todo: assert contents?
@description(
  """Directives are optional settings that affect the execution of the process.
    |""".stripMargin)
@example(
  """directives:
    |    container: rocker/r-ver:4.1
    |    label: highcpu
    |    cpus: 4
    |    memory: 16 GB""".stripMargin,
    "yaml")
case class NextflowDirectives(
  @description(
    """The `accelerator` directive allows you to specify the hardware accelerator requirement for the task execution e.g. GPU processor.
      |
      |Viash implements this directive as a map with accepted keywords: `type`, `limit`, `request`, and `runtime`.
      |
      |See [`accelerator`](https://www.nextflow.io/docs/latest/process.html#accelerator).
      |""".stripMargin)
  @example("""[ limit: 4, type: "nvidia-tesla-k80" ]""", "yaml")
  @default("Empty")
  accelerator: Map[String, String] = Map(),

  @description(
    """The `afterScript` directive allows you to execute a custom (Bash) snippet immediately after the main process has run. This may be useful to clean up your staging area.
      |
      |See [`afterScript`](https://www.nextflow.io/docs/latest/process.html#afterscript).
      |""".stripMargin)
  @example("""source /cluster/bin/cleanup""", "yaml")
  afterScript: Option[String] = None,
  
  @description(
    """The `beforeScript` directive allows you to execute a custom (Bash) snippet before the main process script is run. This may be useful to initialise the underlying cluster environment or for other custom initialisation.
      |
      |See [`beforeScript`](https://www.nextflow.io/docs/latest/process.html#beforeScript).
      |""".stripMargin)
  @example("""source /cluster/bin/setup""", "yaml")
  beforeScript: Option[String] = None,
  
  @description(
    """The `cache` directive allows you to store the process results to a local cache. When the cache is enabled and the pipeline is launched with the resume option, any following attempt to execute the process, along with the same inputs, will cause the process execution to be skipped, producing the stored data as the actual results.
      |
      |The caching feature generates a unique key by indexing the process script and inputs. This key is used to identify univocally the outputs produced by the process execution.
      |
      |The `cache` is enabled by default, you can disable it for a specific process by setting the cache directive to `false`.
      |
      |Accepted values are: `true`, `false`, `"deep"`, and `"lenient"`.
      |
      |See [`cache`](https://www.nextflow.io/docs/latest/process.html#cache).
      |""".stripMargin)
  @example("true", "yaml")
  @example("false", "yaml")
  @example(""""deep"""", "yaml")
  @example(""""lenient"""", "yaml")
  cache: Option[Either[Boolean, String]] = None,
  
  @description(
    """The `conda` directive allows for the definition of the process dependencies using the Conda package manager.
      |
      |Nextflow automatically sets up an environment for the given package names listed by in the `conda` directive.
      |
      |See [`conda`](https://www.nextflow.io/docs/latest/process.html#conda).
      |""".stripMargin)
  @example(""""bwa=0.7.15"""", "yaml")
  @example(""""bwa=0.7.15 fastqc=0.11.5"""", "yaml")
  @example("""["bwa=0.7.15", "fastqc=0.11.5"]""", "yaml")
  @default("Empty")
  conda: OneOrMore[String] = Nil,
  
  @description(
    """The `container` directive allows you to execute the process script in a Docker container.
      |
      |It requires the Docker daemon to be running in machine where the pipeline is executed, i.e. the local machine when using the local executor or the cluster nodes when the pipeline is deployed through a grid executor.
      |
      |Viash implements allows either a string value or a map. In case a map is used, the allowed keys are: `registry`, `image`, and `tag`. The `image` value must be specified.
      |
      |See [`container`](https://www.nextflow.io/docs/latest/process.html#container).
      |""".stripMargin)
  @example(""""foo/bar:tag"""", "yaml")
  @exampleWithDescription("""[ registry: "reg", image: "im", tag: "ta" ]""", "yaml", """This is transformed to `"reg/im:ta"`:""")
  @exampleWithDescription("""[ image: "im" ]""", "yaml", """This is transformed to `"im:latest"`:""")
  container: Option[Either[Map[String, String], String]] = None, // TODO: need to implement container class?
  
  @description(
    """The `containerOptions` directive allows you to specify any container execution option supported by the underlying container engine (ie. Docker, Singularity, etc). This can be useful to provide container settings only for a specific process e.g. mount a custom path.
      |
      |See [`containerOptions`](https://www.nextflow.io/docs/latest/process.html#containeroptions).
      |""".stripMargin)
  @example(""""--foo bar"""", "yaml")
  @example("""["--foo bar", "-f b"]""", "yaml")
  @default("Empty")
  containerOptions: OneOrMore[String] = Nil,
  
  @description(
    """The `cpus` directive allows you to define the number of (logical) CPU required by the process' task.
      |
      |See [`cpus`](https://www.nextflow.io/docs/latest/process.html#cpus).
      |""".stripMargin)
  @example("1", "yaml")
  @example("10", "yaml")
  cpus: Option[Either[Int, String]] = None,
  
  @description(
    """The `disk` directive allows you to define how much local disk storage the process is allowed to use.
      |
      |See [`disk`](https://www.nextflow.io/docs/latest/process.html#disk).
      |""".stripMargin)
  @example(""""1 GB"""", "yaml")
  @example(""""2TB"""", "yaml")
  @example(""""3.2KB"""", "yaml")
  @example(""""10.B"""", "yaml")
  disk: Option[String] = None,
  
  @description(
    """By default the stdout produced by the commands executed in all processes is ignored. By setting the `echo` directive to true, you can forward the process stdout to the current top running process stdout file, showing it in the shell terminal.
      | 
      |See [`echo`](https://www.nextflow.io/docs/latest/process.html#echo).
      |""".stripMargin)
  @example("true", "yaml")
  @example("false", "yaml")
  echo: Option[Either[Boolean, String]] = None,
  
  @description(
    """The `errorStrategy` directive allows you to define how an error condition is managed by the process. By default when an error status is returned by the executed script, the process stops immediately. This in turn forces the entire pipeline to terminate.
      |
      |Table of available error strategies:
      || Name | Executor |
      ||------|----------|
      || `terminate` | Terminates the execution as soon as an error condition is reported. Pending jobs are killed (default) |
      || `finish` | Initiates an orderly pipeline shutdown when an error condition is raised, waiting the completion of any submitted job. |
      || `ignore` | Ignores processes execution errors. |
      || `retry` | Re-submit for execution a process returning an error condition. |
      |
      |See [`errorStrategy`](https://www.nextflow.io/docs/latest/process.html#errorstrategy).
      |""".stripMargin)
  @example(""""terminate"""", "yaml")
  @example(""""finish"""", "yaml")
  errorStrategy: Option[String] = None,
  
  @description(
    """The `executor` defines the underlying system where processes are executed. By default a process uses the executor defined globally in the nextflow.config file.
      |
      |The `executor` directive allows you to configure what executor has to be used by the process, overriding the default configuration. The following values can be used:
      |
      || Name | Executor |
      ||------|----------|
      || awsbatch | The process is executed using the AWS Batch service. | 
      || azurebatch | The process is executed using the Azure Batch service. | 
      || condor | The process is executed using the HTCondor job scheduler. | 
      || google-lifesciences | The process is executed using the Google Genomics Pipelines service. | 
      || ignite | The process is executed using the Apache Ignite cluster. | 
      || k8s | The process is executed using the Kubernetes cluster. | 
      || local | The process is executed in the computer where Nextflow is launched. | 
      || lsf | The process is executed using the Platform LSF job scheduler. | 
      || moab | The process is executed using the Moab job scheduler. | 
      || nqsii | The process is executed using the NQSII job scheduler. | 
      || oge | Alias for the sge executor. | 
      || pbs | The process is executed using the PBS/Torque job scheduler. | 
      || pbspro | The process is executed using the PBS Pro job scheduler. | 
      || sge | The process is executed using the Sun Grid Engine / Open Grid Engine. | 
      || slurm | The process is executed using the SLURM job scheduler. | 
      || tes | The process is executed using the GA4GH TES service. | 
      || uge | Alias for the sge executor. |
      |
      |See [`executor`](https://www.nextflow.io/docs/latest/process.html#executor).
      |""".stripMargin)
  @example(""""local"""", "yaml")
  @example(""""sge"""", "yaml")
  executor: Option[String] = None,
  
  @description(
    """The `label` directive allows the annotation of processes with mnemonic identifier of your choice.
      |
      |See [`label`](https://www.nextflow.io/docs/latest/process.html#label).
      |""".stripMargin)
  @example(""""big_mem"""", "yaml")
  @example(""""big_cpu"""", "yaml")
  @example("""["big_mem", "big_cpu"]""", "yaml")
  @default("Empty")
  label: OneOrMore[String] = Nil,
  
  @description(
    """ The `machineType` can be used to specify a predefined Google Compute Platform machine type when running using the Google Life Sciences executor.
      |
      |See [`machineType`](https://www.nextflow.io/docs/latest/process.html#machinetype).
      |""".stripMargin)
  @example(""""n1-highmem-8"""", "yaml")
  machineType: Option[String] = None,
  
  @description(
    """The `maxErrors` directive allows you to specify the maximum number of times a process can fail when using the `retry` error strategy. By default this directive is disabled.
      |
      |See [`maxErrors`](https://www.nextflow.io/docs/latest/process.html#maxerrors).
      |""".stripMargin)
  @example("1", "yaml")
  @example("3", "yaml")
  maxErrors: Option[Either[String, Int]] = None,
  
  @description(
    """The `maxForks` directive allows you to define the maximum number of process instances that can be executed in parallel. By default this value is equals to the number of CPU cores available minus 1.
      |
      |If you want to execute a process in a sequential manner, set this directive to one.
      |
      |See [`maxForks`](https://www.nextflow.io/docs/latest/process.html#maxforks).
      |""".stripMargin)
  @example("1", "yaml")
  @example("3", "yaml")
  maxForks: Option[Either[String, Int]] = None,
  
  @description(
    """The `maxRetries` directive allows you to define the maximum number of times a process instance can be re-submitted in case of failure. This value is applied only when using the retry error strategy. By default only one retry is allowed.
      |
      |See [`maxRetries`](https://www.nextflow.io/docs/latest/process.html#maxretries).
      |""".stripMargin)
  @example("1", "yaml")
  @example("3", "yaml")
  maxRetries: Option[Either[String, Int]] = None,
  
  @description(
    """The `memory` directive allows you to define how much memory the process is allowed to use.
      |
      |See [`memory`](https://www.nextflow.io/docs/latest/process.html#memory).
      |""".stripMargin)
  @example(""""1 GB"""", "yaml")
  @example(""""2TB"""", "yaml")
  @example(""""3.2KB"""", "yaml")
  @example(""""10.B"""", "yaml")
  memory: Option[String] = None,
  
  @description(
    """Environment Modules is a package manager that allows you to dynamically configure your execution environment and easily switch between multiple versions of the same software tool.
      |
      |If it is available in your system you can use it with Nextflow in order to configure the processes execution environment in your pipeline.
      |
      |In a process definition you can use the `module` directive to load a specific module version to be used in the process execution environment.
      |
      |See [`module`](https://www.nextflow.io/docs/latest/process.html#module).
      |""".stripMargin)
  @example(""""ncbi-blast/2.2.27"""", "yaml")
  @example(""""ncbi-blast/2.2.27:t_coffee/10.0"""", "yaml")
  @example("""["ncbi-blast/2.2.27", "t_coffee/10.0"]""", "yaml")
  @default("Empty")
  module: OneOrMore[String] = Nil,
  
  @description(
    """The `penv` directive allows you to define the parallel environment to be used when submitting a parallel task to the SGE resource manager.
      |
      |See [`penv`](https://www.nextflow.io/docs/latest/process.html#penv).
      |""".stripMargin)
  @example(""""smp"""", "yaml")
  penv: Option[String] = None,
  
  @description(
    """The `pod` directive allows the definition of pods specific settings, such as environment variables, secrets and config maps when using the Kubernetes executor.
      |
      |See [`pod`](https://www.nextflow.io/docs/latest/process.html#pod).
      |""".stripMargin)
  @example("""[ label: "key", value: "val" ]""", "yaml")
  @example("""[ annotation: "key", value: "val" ]""", "yaml")
  @example("""[ env: "key", value: "val" ]""", "yaml")
  @example("""[ [label: "l", value: "v"], [env: "e", value: "v"]]""", "yaml")
  @default("Empty")
  pod: OneOrMore[Map[String, String]] = Nil,
  
  @description(
    """The `publishDir` directive allows you to publish the process output files to a specified folder.
      |
      |Viash implements this directive as a plain string or a map. The allowed keywords for the map are: `path`, `mode`, `overwrite`, `pattern`, `saveAs`, `enabled`. The `path` key and value are required.
      |The allowed values for `mode` are: `symlink`, `rellink`, `link`, `copy`, `copyNoFollow`, `move`.
      |
      |See [`publishDir`](https://www.nextflow.io/docs/latest/process.html#publishdir).
      |""".stripMargin)
  @example("[]", "yaml")
  @example("""[ [ path: "foo", enabled: true ], [ path: "bar", enabled: false ] ]""", "yaml")
  @exampleWithDescription(""""/path/to/dir"""", "yaml", """This is transformed to `[[ path: "/path/to/dir" ]]`:""")
  @exampleWithDescription("""[ path: "/path/to/dir", mode: "cache" ]""", "yaml", """This is transformed to `[[ path: "/path/to/dir", mode: "cache" ]]`:""")
  @default("Empty")
  publishDir: OneOrMore[Either[String, Map[String, String]]] = Nil, // TODO: need to implement publishdir class?
  
  @description(
    """The `queue` directory allows you to set the queue where jobs are scheduled when using a grid based executor in your pipeline.
      |
      |See [`queue`](https://www.nextflow.io/docs/latest/process.html#queue).
      |""".stripMargin)
  @example(""""long"""", "yaml")
  @example(""""short,long"""", "yaml")
  @example("""["short", "long"]""", "yaml")
  @default("Empty")
  queue: OneOrMore[String] = Nil,
  
  @description(
    """The `scratch` directive allows you to execute the process in a temporary folder that is local to the execution node.
      |
      |See [`scratch`](https://www.nextflow.io/docs/latest/process.html#scratch).
      |""".stripMargin)
  @example("true", "yaml")
  @example(""""/path/to/scratch"""", "yaml")
  @example("""'$MY_PATH_TO_SCRATCH'""", "yaml")
  @example(""""ram-disk"""", "yaml")
  scratch: Option[Either[Boolean, String]] = None,
  
  @description(
    """The `storeDir` directive allows you to define a directory that is used as a permanent cache for your process results.
      |
      |See [`storeDir`](https://www.nextflow.io/docs/latest/process.html#storeDir).
      |""".stripMargin)
  @example(""""/path/to/storeDir"""", "yaml")
  storeDir: Option[String] = None,
  
  @description(
    """The `stageInMode` directive defines how input files are staged-in to the process work directory. The following values are allowed:
      |
      || Value | Description |
      ||-------|-------------| 
      || copy | Input files are staged in the process work directory by creating a copy. | 
      || link | Input files are staged in the process work directory by creating an (hard) link for each of them. | 
      || symlink | Input files are staged in the process work directory by creating a symbolic link with an absolute path for each of them (default). | 
      || rellink | Input files are staged in the process work directory by creating a symbolic link with a relative path for each of them. | 
      |
      |See [`stageInMode`](https://www.nextflow.io/docs/latest/process.html#stageinmode).
      |""".stripMargin)
  @example(""""copy"""", "yaml")
  @example(""""link"""", "yaml")
  stageInMode: Option[String] = None,
  
  @description(
    """The `stageOutMode` directive defines how output files are staged-out from the scratch directory to the process work directory. The following values are allowed:
      |
      || Value | Description |
      ||-------|-------------| 
      || copy | Output files are copied from the scratch directory to the work directory. | 
      || move | Output files are moved from the scratch directory to the work directory. | 
      || rsync | Output files are copied from the scratch directory to the work directory by using the rsync utility. |
      |
      |See [`stageOutMode`](https://www.nextflow.io/docs/latest/process.html#stageoutmode).
      |""".stripMargin)
  @example(""""copy"""", "yaml")
  @example(""""link"""", "yaml")
  stageOutMode: Option[String] = None,
  
  @description(
    """The `tag` directive allows you to associate each process execution with a custom label, so that it will be easier to identify them in the log file or in the trace execution report.
      |
      |For ease of use, the default tag is set to `"$id"`, which allows tracking the progression of the channel events through the workflow more easily.
      |
      |See [`tag`](https://www.nextflow.io/docs/latest/process.html#tag).
      |""".stripMargin)
  @example(""""foo"""", "yaml")
  @default("""'$id'""")
  tag: Option[String] = Some("$id"),
  
  @description(
    """The `time` directive allows you to define how long a process is allowed to run.
      |
      |See [`time`](https://www.nextflow.io/docs/latest/process.html#time).
      |""".stripMargin)
  @example(""""1h"""", "yaml")
  @example(""""2days"""", "yaml")
  @example(""""1day 6hours 3minutes 30seconds"""", "yaml")
  time: Option[String] = None
)