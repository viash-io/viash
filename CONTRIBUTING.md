# Contributing

We welcome any contributions to this project, be it changes in documentation, bug fixes, new functionality, or even new packages if they fit the scope of our project.  By participating in this project, you
agree to abide by our [code of conduct](CONDUCT.md).

## Getting Started

* If you don't have one already, create a [GitHub account](https://github.com/signup/join).

* Search for an existing issue for your problem or suggestion in the [issue tracker](https://github.com/viash-io/viash/issues). If you find a related issue, please comment there rather than creating a new issue. If none can be found, please [create a new issue](https://github.com/viash-io/viash/issues/new) for your problem or suggestion. Clearly describe your issue, including steps to reproduce when it is a bug, or some justification for a proposed improvement.

## Requirements

* [Java 11](https://www.oracle.com/java/technologies/javase-jdk11-downloads.html) or greater

* [sbt](https://www.scala-sbt.org/)

The following dependencies are required for passing some of the tests.

* [Python 3.10](https://www.python.org/downloads/) or greater

* [R 4.0](https://www.r-project.org/) or greater

* [Nextflow](https://www.nextflow.io/) 22.04.5 or greater

* [Docker](https://www.docker.com/)

## Forking the code

* [Fork](https://github.com/viash-io/viash/#fork-destination-box) the repository on GitHub to make a copy of the repository on your account.

* Clone your fork to your local machine so you can edit the files. Make sure to check out the `develop` branch, since this is the branch where all development happens.

  ```bash
  git clone https://github.com/your-username/viash.git
  cd viash
  git checkout develop
  ```

* Create a new branch for your changes. It's best to keep your changes separate from the `develop` branch.

## Making changes

* Code Standards: Ensure your code adheres to the [Scala style guide](https://docs.scala-lang.org/style/).

* Write Tests: If you add new features or fix bugs, write tests that cover your changes. Our project uses sbt for testing.

* Run Tests Locally: Before submitting your changes, make sure all tests pass locally. Our GitHub Actions CI pipeline performs tests on different environments, but it's good practice to check everything beforehand.

  ```bash
  sbt test
  ```

  If you couldn't install some of the required dependencies, you can skip the tests that require them. For example, to skip all tests requiring Docker and Nextflow:

  ```bash
  sbt 'testOnly -- -l io.viash.DockerTest -l io.viash.NextflowTest'
  ```

* Documentation: Update the documentation if your changes require it. This includes both in-code documentation and external documentation like README.md.

## Submitting your changes

* Update the Changelog: Add a new entry to the [CHANGELOG.md](CHANGELOG.md) file that describes your changes. Follow the existing format for entries.

* Commit Your Changes: Once you're happy with your changes, commit them to your branch.

  ```sh
  git commit -m "Add a brief description of your changes"
  ```

* Push to Your Fork: Push your changes to your fork on GitHub.

  ```sh
  git push origin your-branch-name
  ```

* Create a Pull Request: Go to the viash-io/viash repository on GitHub and create a new pull request. Describe your changes and submit it for review.

## Review Process

Our team will review your pull request. We might ask for changes or clarifications. Keep an eye on your pull request for feedback.

## Additional information

### Creating a build

To create a build of Viash and install it in `~/.local/bin`, run the following commands:

```bash
./configure --prefix=~/.local
make
make install
```