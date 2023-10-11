def assertMapKeys(map, expectedKeys, requiredKeys, mapName) {
  assert map instanceof Map : "Expected argument '$mapName' to be a Map. Found: class ${map.getClass()}"
  map.forEach { key, val -> 
    assert key in expectedKeys : "Unexpected key '$key' in ${mapName ? mapName + " " : ""}map"
  }
  requiredKeys.forEach { requiredKey -> 
    assert map.containsKey(requiredKey) : "Missing required key '$key' in ${mapName ? mapName + " " : ""}map"
  }
}

// TODO: unit test processDirectives
def processDirectives(Map drctv) {
  // remove null values
  drctv = drctv.findAll{k, v -> v != null}

  // check for unexpected keys
  def expectedKeys = [
    "accelerator", "afterScript", "beforeScript", "cache", "conda", "container", "containerOptions", "cpus", "disk", "echo", "errorStrategy", "executor", "machineType", "maxErrors", "maxForks", "maxRetries", "memory", "module", "penv", "pod", "publishDir", "queue", "label", "scratch", "storeDir", "stageInMode", "stageOutMode", "tag", "time"
  ]
  def unexpectedKeys = drctv.keySet() - expectedKeys
  assert unexpectedKeys.isEmpty() : "Unexpected keys in process directive: '${unexpectedKeys.join("', '")}'"

  /* DIRECTIVE accelerator
    accepted examples:
    - [ limit: 4, type: "nvidia-tesla-k80" ]
  */
  if (drctv.containsKey("accelerator")) {
    assertMapKeys(drctv["accelerator"], ["type", "limit", "request", "runtime"], [], "accelerator")
  }

  /* DIRECTIVE afterScript
    accepted examples:
    - "source /cluster/bin/cleanup"
  */
  if (drctv.containsKey("afterScript")) {
    assert drctv["afterScript"] instanceof CharSequence
  }

  /* DIRECTIVE beforeScript
    accepted examples:
    - "source /cluster/bin/setup"
  */
  if (drctv.containsKey("beforeScript")) {
    assert drctv["beforeScript"] instanceof CharSequence
  }

  /* DIRECTIVE cache
    accepted examples:
    - true
    - false
    - "deep"
    - "lenient"
  */
  if (drctv.containsKey("cache")) {
    assert drctv["cache"] instanceof CharSequence || drctv["cache"] instanceof Boolean
    if (drctv["cache"] instanceof CharSequence) {
      assert drctv["cache"] in ["deep", "lenient"] : "Unexpected value for cache"
    }
  }

  /* DIRECTIVE conda
    accepted examples:
    - "bwa=0.7.15"
    - "bwa=0.7.15 fastqc=0.11.5"
    - ["bwa=0.7.15", "fastqc=0.11.5"]
  */
  if (drctv.containsKey("conda")) {
    if (drctv["conda"] instanceof List) {
      drctv["conda"] = drctv["conda"].join(" ")
    }
    assert drctv["conda"] instanceof CharSequence
  }

  /* DIRECTIVE container
    accepted examples:
    - "foo/bar:tag"
    - [ registry: "reg", image: "im", tag: "ta" ]
      is transformed to "reg/im:ta"
    - [ image: "im" ] 
      is transformed to "im:latest"
  */
  if (drctv.containsKey("container")) {
    assert drctv["container"] instanceof Map || drctv["container"] instanceof CharSequence
    if (drctv["container"] instanceof Map) {
      def m = drctv["container"]
      assertMapKeys(m, [ "registry", "image", "tag" ], ["image"], "container")
      def part1 = 
        System.getenv('OVERRIDE_CONTAINER_REGISTRY') ? System.getenv('OVERRIDE_CONTAINER_REGISTRY') + "/" : 
        params.containsKey("override_container_registry") ? params["override_container_registry"] + "/" : // todo: remove?
        m.registry ? m.registry + "/" : 
        ""
      def part2 = m.image
      def part3 = m.tag ? ":" + m.tag : ":latest"
      drctv["container"] = part1 + part2 + part3
    }
  }

  /* DIRECTIVE containerOptions
    accepted examples:
    - "--foo bar"
    - ["--foo bar", "-f b"]
  */
  if (drctv.containsKey("containerOptions")) {
    if (drctv["containerOptions"] instanceof List) {
      drctv["containerOptions"] = drctv["containerOptions"].join(" ")
    }
    assert drctv["containerOptions"] instanceof CharSequence
  }

  /* DIRECTIVE cpus
    accepted examples:
    - 1
    - 10
  */
  if (drctv.containsKey("cpus")) {
    assert drctv["cpus"] instanceof Integer
  }

  /* DIRECTIVE disk
    accepted examples:
    - "1 GB"
    - "2TB"
    - "3.2KB"
    - "10.B"
  */
  if (drctv.containsKey("disk")) {
    assert drctv["disk"] instanceof CharSequence
    // assert drctv["disk"].matches("[0-9]+(\\.[0-9]*)? *[KMGTPEZY]?B")
    // ^ does not allow closures
  }

  /* DIRECTIVE echo
    accepted examples:
    - true
    - false
  */
  if (drctv.containsKey("echo")) {
    assert drctv["echo"] instanceof Boolean
  }

  /* DIRECTIVE errorStrategy
    accepted examples:
    - "terminate"
    - "finish"
  */
  if (drctv.containsKey("errorStrategy")) {
    assert drctv["errorStrategy"] instanceof CharSequence
    assert drctv["errorStrategy"] in ["terminate", "finish", "ignore", "retry"] : "Unexpected value for errorStrategy"
  }

  /* DIRECTIVE executor
    accepted examples:
    - "local"
    - "sge"
  */
  if (drctv.containsKey("executor")) {
    assert drctv["executor"] instanceof CharSequence
    assert drctv["executor"] in ["local", "sge", "uge", "lsf", "slurm", "pbs", "pbspro", "moab", "condor", "nqsii", "ignite", "k8s", "awsbatch", "google-pipelines"] : "Unexpected value for executor"
  }

  /* DIRECTIVE machineType
    accepted examples:
    - "n1-highmem-8"
  */
  if (drctv.containsKey("machineType")) {
    assert drctv["machineType"] instanceof CharSequence
  }

  /* DIRECTIVE maxErrors
    accepted examples:
    - 1
    - 3
  */
  if (drctv.containsKey("maxErrors")) {
    assert drctv["maxErrors"] instanceof Integer
  }

  /* DIRECTIVE maxForks
    accepted examples:
    - 1
    - 3
  */
  if (drctv.containsKey("maxForks")) {
    assert drctv["maxForks"] instanceof Integer
  }

  /* DIRECTIVE maxRetries
    accepted examples:
    - 1
    - 3
  */
  if (drctv.containsKey("maxRetries")) {
    assert drctv["maxRetries"] instanceof Integer
  }

  /* DIRECTIVE memory
    accepted examples:
    - "1 GB"
    - "2TB"
    - "3.2KB"
    - "10.B"
  */
  if (drctv.containsKey("memory")) {
    assert drctv["memory"] instanceof CharSequence
    // assert drctv["memory"].matches("[0-9]+(\\.[0-9]*)? *[KMGTPEZY]?B")
    // ^ does not allow closures
  }

  /* DIRECTIVE module
    accepted examples:
    - "ncbi-blast/2.2.27"
    - "ncbi-blast/2.2.27:t_coffee/10.0"
    - ["ncbi-blast/2.2.27", "t_coffee/10.0"]
  */
  if (drctv.containsKey("module")) {
    if (drctv["module"] instanceof List) {
      drctv["module"] = drctv["module"].join(":")
    }
    assert drctv["module"] instanceof CharSequence
  }

  /* DIRECTIVE penv
    accepted examples:
    - "smp"
  */
  if (drctv.containsKey("penv")) {
    assert drctv["penv"] instanceof CharSequence
  }

  /* DIRECTIVE pod
    accepted examples:
    - [ label: "key", value: "val" ]
    - [ annotation: "key", value: "val" ]
    - [ env: "key", value: "val" ]
    - [ [label: "l", value: "v"], [env: "e", value: "v"]]
  */
  if (drctv.containsKey("pod")) {
    if (drctv["pod"] instanceof Map) {
      drctv["pod"] = [ drctv["pod"] ]
    }
    assert drctv["pod"] instanceof List
    drctv["pod"].forEach { pod ->
      assert pod instanceof Map
      // TODO: should more checks be added?
      // See https://www.nextflow.io/docs/latest/process.html?highlight=directives#pod
      // e.g. does it contain 'label' and 'value', or 'annotation' and 'value', or ...?
    }
  }

  /* DIRECTIVE publishDir
    accepted examples:
    - []
    - [ [ path: "foo", enabled: true ], [ path: "bar", enabled: false ] ]
    - "/path/to/dir" 
      is transformed to [[ path: "/path/to/dir" ]]
    - [ path: "/path/to/dir", mode: "cache" ]
      is transformed to [[ path: "/path/to/dir", mode: "cache" ]]
  */
  // TODO: should we also look at params["publishDir"]?
  if (drctv.containsKey("publishDir")) {
    def pblsh = drctv["publishDir"]
    
    // check different options
    assert pblsh instanceof List || pblsh instanceof Map || pblsh instanceof CharSequence
    
    // turn into list if not already so
    // for some reason, 'if (!pblsh instanceof List) pblsh = [ pblsh ]' doesn't work.
    pblsh = pblsh instanceof List ? pblsh : [ pblsh ]

    // check elements of publishDir
    pblsh = pblsh.collect{ elem ->
      // turn into map if not already so
      elem = elem instanceof CharSequence ? [ path: elem ] : elem

      // check types and keys
      assert elem instanceof Map : "Expected publish argument '$elem' to be a String or a Map. Found: class ${elem.getClass()}"
      assertMapKeys(elem, [ "path", "mode", "overwrite", "pattern", "saveAs", "enabled" ], ["path"], "publishDir")

      // check elements in map
      assert elem.containsKey("path")
      assert elem["path"] instanceof CharSequence
      if (elem.containsKey("mode")) {
        assert elem["mode"] instanceof CharSequence
        assert elem["mode"] in [ "symlink", "rellink", "link", "copy", "copyNoFollow", "move" ]
      }
      if (elem.containsKey("overwrite")) {
        assert elem["overwrite"] instanceof Boolean
      }
      if (elem.containsKey("pattern")) {
        assert elem["pattern"] instanceof CharSequence
      }
      if (elem.containsKey("saveAs")) {
        assert elem["saveAs"] instanceof CharSequence //: "saveAs as a Closure is currently not supported. Surround your closure with single quotes to get the desired effect. Example: '\{ foo \}'"
      }
      if (elem.containsKey("enabled")) {
        assert elem["enabled"] instanceof Boolean
      }

      // return final result
      elem
    }
    // store final directive
    drctv["publishDir"] = pblsh
  }

  /* DIRECTIVE queue
    accepted examples:
    - "long"
    - "short,long"
    - ["short", "long"]
  */
  if (drctv.containsKey("queue")) {
    if (drctv["queue"] instanceof List) {
      drctv["queue"] = drctv["queue"].join(",")
    }
    assert drctv["queue"] instanceof CharSequence
  }

  /* DIRECTIVE label
    accepted examples:
    - "big_mem"
    - "big_cpu"
    - ["big_mem", "big_cpu"]
  */
  if (drctv.containsKey("label")) {
    if (drctv["label"] instanceof CharSequence) {
      drctv["label"] = [ drctv["label"] ]
    }
    assert drctv["label"] instanceof List
    drctv["label"].forEach { label ->
      assert label instanceof CharSequence
      // assert label.matches("[a-zA-Z0-9]([a-zA-Z0-9_]*[a-zA-Z0-9])?")
      // ^ does not allow closures
    }
  }

  /* DIRECTIVE scratch
    accepted examples:
    - true
    - "/path/to/scratch"
    - '$MY_PATH_TO_SCRATCH'
    - "ram-disk"
  */
  if (drctv.containsKey("scratch")) {
    assert drctv["scratch"] == true || drctv["scratch"] instanceof CharSequence
  }

  /* DIRECTIVE storeDir
    accepted examples:
    - "/path/to/storeDir"
  */
  if (drctv.containsKey("storeDir")) {
    assert drctv["storeDir"] instanceof CharSequence
  }

  /* DIRECTIVE stageInMode
    accepted examples:
    - "copy"
    - "link"
  */
  if (drctv.containsKey("stageInMode")) {
    assert drctv["stageInMode"] instanceof CharSequence
    assert drctv["stageInMode"] in ["copy", "link", "symlink", "rellink"]
  }

  /* DIRECTIVE stageOutMode
    accepted examples:
    - "copy"
    - "link"
  */
  if (drctv.containsKey("stageOutMode")) {
    assert drctv["stageOutMode"] instanceof CharSequence
    assert drctv["stageOutMode"] in ["copy", "move", "rsync"]
  }

  /* DIRECTIVE tag
    accepted examples:
    - "foo"
    - '$id'
  */
  if (drctv.containsKey("tag")) {
    assert drctv["tag"] instanceof CharSequence
  }

  /* DIRECTIVE time
    accepted examples:
    - "1h"
    - "2days"
    - "1day 6hours 3minutes 30seconds"
  */
  if (drctv.containsKey("time")) {
    assert drctv["time"] instanceof CharSequence
    // todo: validation regex?
  }

  return drctv
}
