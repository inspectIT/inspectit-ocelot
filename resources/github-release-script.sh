#!/bin/bash

# Based on the shell script of Github user Jaskaranbir (https://gist.github.com/Jaskaranbir/d5b065173b3a6f164e47a542472168c1)

# ===> Set these variables first
branch="master"
repo_slug="$TRAVIS_REPO_SLUG"
token="$GITHUB_TOKEN"
version="$TRAVIS_TAG"

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

echo "You can also find the corresponding documentation online under the following link: [Documentation $version](http://docs.inspectit.rocks/releases/$version)" > release_body.md
echo "" >> release_body.md
cat CHANGELOG.md >> release_body.md

body="$(cat release_body.md)"

# Overwrite CHANGELOG.md with JSON data for GitHub API
jq -n \
  --arg body "$body" \
  --arg name "Version $version" \
  --arg tag_name "$version" \
  --arg target_commitish "$branch" \
  '{
    body: $body,
    name: $name,
    tag_name: $tag_name,
    target_commitish: $target_commitish,
    draft: false,
    prerelease: false
  }' > CHANGELOG.md

echo "Create release $version for repo: $repo_slug, branch: $branch"
curl -H "Authorization: token $token" --data @CHANGELOG.md "https://api.github.com/repos/$repo_slug/releases"