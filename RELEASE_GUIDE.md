# Prepare the develop branch for release

## Set the version in `build.sbt`

e.g. from `version := "0.7.2-dev"` to `version := "0.7.2"`

## Finalize the changelog changes

- Set the correct version number
- Set the current date
- Provide a title
- Add a summary

## Create a PR from develop to main

# Merge PR to main

# Release Viash

## Run 'Prepare Viash Release' workflow on GitHub

Select the `main` branch

## Release the prepared release

Head over releases and select the prepared release.

Double check the tag version.

Add the title from the changelog.md into the release notes.

Select the necessary prerelease or latest release options and hit the 'publish release' button.

# Update the website

The source repository is [here](https://github.com/viash-io/website).
Follow the information in the README.md to get up and running if needed.

## Update _viash.yaml

Update the viash version to the latest Viash version.
This is needed to get updated information about the CLI and JSON schema.

## Rerender Website

Short summary if you already have an operational envirionment:

```bash
./_src/render_pages.sh 
```

(This script already contains the environment switch command (`source renv/python/virtualenvs/renv-python-3.10/bin/activate`))

## Create a new branch, push to GitHub and create a PR to main

## Merge website PR

# Post merge

Under the `develop` branch,
- Update the version in `build.sbt` to the next version with a `-dev` suffix.
- Add a placeholder entry in `CHANGELOG.md` for a future release.

Template:

```
# Viash 0.x.x (yyyy-MM-dd): TODO Add title

TODO add summary
```

