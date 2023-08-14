# Prepare the develop branch for release

## Set the version in `build.sbt`

e.g. from `version := "0.7.2 dev"` to `version := "0.7.2"`

## Finalize the changelog changes

- Set the correct version number
- Set the current date
- Provide a title
- Add a summary

## Create a PR from develop to main

# Merge PR to main

## Add a tag on main with the new version number

# Create GitHub release

Title: 'Viash 0.7.2'
Content: Add the full changelog entry of this release

# Build viash and viash_install and add to the release

Create the binaries with `make && make tools`.
Add the files `bin/viash` and `bin/viash_install`.

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
- Update the version in `build.sbt` to the next version with a `dev` suffix.
- Add a placeholder entry in `CHANGELOG.md` for a future release.

Template:

```
# Viash 0.x.x (yyyy-MM-dd): TODO Add title

TODO add summary
```

