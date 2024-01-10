---
title: 'Viash: A meta-framework for building reusable workflow modules'
bibliography: library.bib
authors:
- name: Robrecht Cannoodt
  email: robrecht@data-intuitive.com
  orcid: 0000-0003-3641-729X
  corresponding: yes
  affiliation: 1, 2, 3
- name: Hendrik Cannoodt
  orcid: 0000-0002-0032-6719
  affiliation: '1'
- name: Dries Schaumont
  orcid: 0000-0002-4389-0440
  affiliation: '1'
- name: Kai Waldrant
  orcid: 0009-0003-8555-1361
  affiliation: '1'
- name: Eric Van de Kerckhove
  affiliation: '1'
- name: Andy Boschmans
  orcid: 0009-0008-8793-4661
  affiliation: '1'
- name: Dries De Maeyer
  orcid: 0000-0002-1314-3348
  affiliation: '4'
- name: Toni Verbeiren
  email: toni@data-intuitive.com
  orcid: 0000-0002-7007-6866
  corresponding: yes
  affiliation: '1'
affiliations:
- name: Data Intuitive, Lebbeke, Belgium
  index: 1
- name: Data Mining and Modelling for Biomedicine group, VIB Center for Inflammation
    Research, Ghent, Belgium
  index: 2
- name: Department of Applied Mathematics, Computer Science, and Statistics, Ghent
    University, Ghent, Belgium
  index: 3
- name: Discovery Technology and Molecular Pharmacology, Janssen Research & Development,
    Pharmaceutical Companies of Johnson & Johnson, Beerse, Belgium
  index: 4
tags:
- Bioinformatics
- Workflows
- Software development
- Reproducibility
- Cloud computing
- Docker
- Nextflow
---





# Abstract
Most bioinformatics workflows consist of software components that are tightly coupled to the logic of the workflow itself. This limits reusability of the individual components in the workflow or introduces maintenance overhead when they need to be reimplemented in multiple workflows. We introduce Viash, a tool for speeding up development of robust workflows through "code-first" prototyping, separation of concerns and code generation of modular workflow components. By decoupling the component functionality from the workflow logic, component functionality becomes fully workflow-agnostic, and conversely the resulting workflows are agnostic towards specific component requirements. This separation of concerns improves reusability of components and facilitates multidisciplinary and pan-organisational collaborations. It has been applied in a variety of projects, from proof-of-concept workflows to supporting an international data science competition.

Viash is available as an open-source project at [github.com/viash-io/viash](https://github.com/viash-io/viash) and documentation is available at [viash.io](https://viash.io).



# Statement of Need
Recent developments in high-throughput RNA sequencing and imaging technologies allow present-day biologists to observe single-cell characteristics in ever more detail [@luecken_currentbestpractices_2019]. As the dataset size and the complexity of bioinformatics workflows increases, so does the need for scalable and reproducible data science. In single cell biology, recent efforts to standardise some of the most common single-cell analyses [@amezquita_orchestratingsinglecell_2020; @nfcoreframeworkcommunity_ewels2020; @huemos_bestpracticessingle_2023] tackle these challenges by using a workflow framework (e.g. Snakemake, Nextflow), containerisation (e.g. Docker, Podman) and horizontal scaling in cloud computing (e.g. Kubernetes, HPC).

Since research projects are increasingly more complex and interdisciplinary, researchers from different fields and backgrounds are required to join forces. This implies that not all project contributors can be experts in computer science. The chosen framework for such projects therefore needs to have a low barrier to entry in order for contributors to be able to participate. One common pitfall which greatly increases the barrier to entry is tightly coupling a workflow and the components it consists of. Major drawbacks include lower transparency of the overall workflow, limited reusability of workflow components, increased complexity, debugging time increase and a greater amount of time spent refactoring and maintaining boilerplate code. Non-expert developers in particular will experience more arduous debugging sessions as they need to treat the workflow as a black box.

In this work we introduce Viash, a tool for speeding up workflow prototyping through code generation, component modularity and separation of concerns. With Viash, a user can create a workflow module by writing a small script or using a pre-existing code block, adding a small amount of metadata, and using Viash to generate the boilerplate code needed to turn it into a modular Nextflow component. This separates the component functionality from the workflow workflow, thereby allowing a component developer to focus on implementing the required functionality using the domain-specific toolkit at hand while being completely agnostic to the chosen workflow framework. Similarly, a workflow developer can design a workflow by chaining together Viash modules while being completely agnostic to the scripting language used in the component.



# Core features and functionality
Viash is an open-source embodiment of a ‘code-first’ concept for workflow development. Many bioinformatics research projects (and other software development projects) start with prototyping functionality in small scripts or notebooks in order to then migrate the functionality to software packages or workflow frameworks. By adding some metadata to a code block or script (\autoref{fig-overview}A), Viash can turn a (small) code block into a highly malleable object. By encapsulating core functionality in modular building blocks, a Viash component can be used in a myriad of ways (\autoref{fig-overview}B-C): export it as a standalone command-line tool; create a highly intuitive and modular Nextflow component; ensure reproducibility by building, pulling, or pushing Docker containers; or running one or more unit tests to verify that the component works as expected. Integration with CI tools such as GitHub Actions, Jenkins or Travis CI allows for automation of unit testing, rolling releases and versioned releases.

The definition of a Viash component -- a config and a code block -- can be implemented quite concisely (\autoref{fig-cheatsheet} left). Viash currently supports different scripting languages, including Bash, JavaScript, Python and R. Through the use of several subcommands (\autoref{fig-cheatsheet} right), Viash can build the component into a standalone script using one of three backend platforms -- native, Docker, or Nextflow. Additional commands allow processing one or more Viash components simultaneously, e.g. for executing a unit test suite or (re-)building component-specific Docker images.




![Viash allows easy prototyping of reusable workflow components. **A:** Viash requires two main inputs, a script (or code block) and a Viash config file. A Viash config file is a YAML file with metadata describing the functionality provided by the component (e.g. a name and description of the component and its parameters), and platform-specific metadata (e.g. the base Docker container to use, which software packages are required by the component). Optionally, the quality of the component can be improved by defining one or more unit tests with which the component functionality can be tested. **B:** Viash allows transforming a given config to a variety of different outputs. **C:** Viash supports robust workflow development by allowing users to build their component as a standalone executable (with auto-generated CLI), build a Docker image to run the script inside, or turn the component into a standalone Nextflow module or workflow. If unit tests were defined, Viash can also run all of the unit tests and provide users with a report.\label{fig-overview}](figures/figure_2.pdf)


One major benefit of using code regeneration is that best practices in workflow development can automatically be applied, whereas otherwise this would be left up to the developer to develop and maintain. For instance, all standalone executables, Nextflow modules and Docker images are automatically versioned. When parsing command-line arguments, checking for the availability of required parameters, the existence of required input files, or the type-checking of command-line arguments is also automated.  Another example is helper functions for installing software through tools such as apt, apk, yum, pip or R devtools, as these sometimes require additional pre-install commands to update package registries or post-install commands to clean up the installation cache to reduce image size of the resulting image. Here, Viash could be the technical basis for a community of people committed to sharing components that everybody can benefit from.




![Cheat sheet for developing modular workflow components with Viash, including a sample Viash component (**left**) and common commands used throughout the various stages of a development cycle (**right**).\label{fig-cheatsheet}](figures/figure_3.pdf)



# State of the field

The realm of bioinformatics workflow management is evolving rapidly, with numerous frameworks and portability solutions emerging to address the escalating complexity and scale of data processing [@wratten_reproduciblescalablesharable_2021]. Viash positions itself uniquely in this landscape as a meta-framework, focusing on the creation of portable workflow modules.

Workflow frameworks can be broadly categorised into three broad categories: Graphical, Programmatic, and Specification-based types. Graphical workflow frameworks such as Galaxy [@goecks_galaxycomprehensiveapproach_2010] and KNIME [@fillburn_knimereproduciblecrossdomain_2017] are user-friendly for non-coders, while programmatic workflow frameworks such as Nextflow [@ditommaso_nextflowenablesreproducible_2017], Snakemake [@koster_snakemakescalablebioinformaticsworkflow_2012], and WDL ([https://openwdl.org](https://openwdl.org)) offer a DSL or programming library for developers. Specification-based workflow frameworks such as CWL [@crusoe_methodsincludestandardizing_2022] lie somewhere in between. These allow to describe and execute workflows with specification files (e.g. a YAML), and these specification files can be constructed using Graphical or Programmatic interfaces.

Portability solutions are critical for ensuring reproducibility. These can be divided into package managers like Conda for automated installation of versioned software, and containerization tools like Docker ([https://www.docker.com](https://www.docker.com)) and Podman [@heon_podmantoolmanaging_2018], which package and distribute software dependencies in a self-contained and platform-independent manner.

Viash's role in this landscape is to bridge these diverse tools, enabling more efficient and collaborative development in bioinformatics workflows.



# Applications in bioinformatics
Ultimately, Viash aims to support pan-organisational and interdisciplinary research projects by simplifying collaborative development and maintenance of (complex) workflows. While Viash is generally applicable to any field where scalable and reproducible data processing workflows are needed, one field where it is particularly useful is in single-cell bioinformatics since it supports most of the commonly used technologies in this field, namely Bash, Python, R, Docker, and Nextflow.

The OpenProblems-NeurIPS2021 organised by OpenProblems demonstrates the practical value of Viash [@luecken_sandboxpredictionintegration_2021]. As part of the preparation for the competition, a pilot benchmark was implemented to evaluate and compare the performance of a few baseline methods (\autoref{fig-usecase}A). By pre-defining the input-output interfaces of several types of components (e.g. dataset loaders, baseline methods, control methods, metrics), developers from different organisations across the globe could easily contribute Viash components to the workflow (\autoref{fig-usecase}B). Since Viash automatically generates Docker containers and Nextflow workflows from the meta-data provided by component developers, developers could contribute components whilst making use of their programming environment of choice without needing to have any expert knowledge of Nextflow or Amazon EC2 (\autoref{fig-usecase}C). Thanks to the modularity of Viash components, the same components used in running a pilot benchmark are also used by the evaluation worker of the competition website itself. As such, the pilot benchmark also serves as an integration test of the evaluation worker.




![A recent NeurIPS competition for multimodal data integration [@luecken_sandboxpredictionintegration_2021] demonstrates the practical value of Viash by using Bash, R, Python, Docker, Nextflow, Viash, and Amazon EC2 as core technologies to run a pilot benchmark. **A:** The pilot benchmark workflow consists of several types of components, each of which had strict predefined input-output interfaces. **B:** Comparing which organisations contributed one or more Viash components to the workflow demonstrates that Viash allows multiple organisations to participate in developing a workflow collaboratively. Note: this visualisation pertains to one aspect of organising the NeurIPS competition, and does not at all reflect the overall efforts made by any party. **C:** Developers are encouraged to implement components in their preferred scripting language. Thanks to the modularity provided by Viash, sewing together multiple components into a Nextflow workflow can be left up to a few developers, without requiring all collaborators to have expert knowledge regarding infrastructure-specific technologies.\label{fig-usecase}](figures/figure_4.pdf)


# Discussion
Viash is under active development. Active areas of development include expanded compatibility between Viash and other technologies (i.e. additional scripting languages, containerisation frameworks and workflow frameworks), and ease-of-use functionality for developing and managing  large catalogues of Viash components (e.g. simplified continuous integration, allowing project-wide settings, automating versioned releases).

We appreciate and encourage contributions to or extensions of Viash. All source code is available under a GPL-3 licence on Github at [github.com/viash-io/viash](https://github.com/viash-io/viash). Extensive user documentation is available at [viash.io](https://viash.io). Requests for support or expanded functionality can be addressed to the corresponding authors.

 


# References




