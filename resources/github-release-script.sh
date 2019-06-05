#!/bin/bash

# Based on the shell script of Github user Jaskaranbir (https://gist.github.com/Jaskaranbir/d5b065173b3a6f164e47a542472168c1)

# ===> Set these variables first
branch="master"
repo_slug="inspectIT/inspectit-ocelot"
token="$GITHUB_TOKEN"
version="$CIRCLE_TAG"

LAST_REVISION=$(git rev-list --tags --skip=1 --max-count=1)
LAST_RELEASE_TAG=$(git describe --abbrev=0 --tags ${LAST_REVISION})

# Generate CHANGELOG.md
github_changelog_generator \
  -u $(cut -d "/" -f1 <<< $repo_slug) \
  -p $(cut -d "/" -f2 <<< $repo_slug) \
  --token $token \
  --since-tag ${LAST_RELEASE_TAG} \
  --no-author \
  --no-unreleased \
  --header-label "## Changelog"

sed -i -e '3,5d' CHANGELOG.md
sed -i '$ d' CHANGELOG.md

echo "You can also find the corresponding documentation online under the following link: [inspectIT Ocelot Documentation](http://docs.inspectit.rocks)" > release_body.md
echo "" >> release_body.md
cat CHANGELOG.md >> release_body.md