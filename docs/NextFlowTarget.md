# NextFlow Target

## Introduction

## Filenames

It's important in a pipeline that input and output filenames are explicitly declared. We try to generalize this as much as possible keeping in mind that no clashes between filenames (in parallel runs) should collide.

In order to do that, we make the assumption that a module `moduleX` transforms the filenames in the following way:

```
file.ext      -> file.moduleX.ext'
file.int.ext  -> file.moduleX.ext'
```

At least, for modules/functions that turn one input file into one output file (not taking into account logging etc.).

## `function_type`

The `function_type` attempts to capture some high-level functionality of the _function_ at hand. There are two things to consider for this:

- What is the input/output signature of the function? In other words, is the input one or more files? Is the output one or more files?
- How should the output files be named (based on the input)

For instance, a tool that creates a full directory structure as output has a different _signature_ than one that unzips are file.

### `asis`

__TODO__

### `transform`

The `transform` type modifies a file such that the result is of the same type as the original.

Examples are:

- A TOC is added to a Markdown file. Bot input and output are Markdown format.
- An `h5ad` file is loaded and additional annotations are added to it.

### `convert`

The `convert` function type converts one format into an other. The type of a file is usually expressed by means of the extension. The extension of the target file is based upon the extension of the default output file value as specified in `functionality.yaml`.

Examples are:

- Generate a PDF file from a Markdown.
- Convert an `h5ad` file into a `loom` file

Please note that `transform` and `convert` are implemented in exactly the same way, so they can be interchanged.
In a future version of the NXF Target, we plan on allowing users to specify the output file name by means of other ways.

### `todir`

In this case, a tool writes multiple files and we ideally store those in a dedicated (sub) directory. This could correspond to a _fork_ in the pipeline DAG, but not necessarily so.

### `join`

A pipeline DAG usually contains forks and joins. This function type combines things. Tools like, e.g. Pandoc, allow the user to combine several files into one output file.

## Specific Functionality

### Labels

NXF allows the use of labels for processes as a way to refer to a set of processes by means of this label. In order to use this functionality, add the following to the NXF `platform.yaml`:

```yaml
label: myFancyLabel
```

And then in the main `nextflow.config`, use:

```
process {
  ...
  withLabel: myFancyLabel {
     maxForks = 5
     ...
  }
}
```

### Publish or not

One does not always want to keep all intermediate results. Usually you want to keep the end results and for instance reports that are generated.

The default is to publish everything, but if you specifically do _not_ want the output of a step to be published, use:

```yaml
publish: false
```

Please note that NXF always keeps a cache of all input/output files for allowing `-resume` runs.

### PublishDir

In some cases it may make sense to have the output data _published_ (not cached) in a directory _per sample_. This can be achieved using the following attribute:

```yaml
publishSubDir: true
```

## TODO

### Packages

The NXF target inherits the R and Python platforms. This should _in principle_ allow a user to specify additional packages to be installed prior to running the function/module. This functionality is not yet available, though.


