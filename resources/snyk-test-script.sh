#!/bin/bash

snyk test --file=build.gradle --all-sub-projects --sarif-file-output=snyk_test_result.sarif
exit_code=$?

if (( exit_code == 0)); then
  echo "No issues found by Snyk. Will upload SARIF."
  (exit 0)
elif (( exit_code == 1)); then
  echo "Issues found by Snyk. Will upload SARIF."
  (exit 0)
else
  echo "Snyk command failed. Will not upload SARIF."
  (exit 1)
fi
