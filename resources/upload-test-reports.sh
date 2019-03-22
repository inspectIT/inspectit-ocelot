#!/bin/bash

# Create test report archive and upload it to transfer.sh

echo "Packing and uploading test-reports"

ARCHIVE_FILE_NAME="inspectit-ocelot-test-reports-$TRAVIS_JOB_NUMBER.zip"
ARCHIVE_FILE="$TRAVIS_BUILD_DIR/$ARCHIVE_FILE_NAME"

DIRECTORY="$TRAVIS_BUILD_DIR/inspectit-ocelot-agent/build/reports/tests/"
if [ -d "$DIRECTORY" ]; then
    echo "Packing test-reports of agent project.."
    cd "$DIRECTORY"
    mv ./* agent
    zip -q -r "$ARCHIVE_FILE" .
fi

DIRECTORY="$TRAVIS_BUILD_DIR/inspectit-ocelot-core/build/reports/tests/"
if [ -d "$DIRECTORY" ]; then
    echo "Packing test-reports of core project.."
    cd "$DIRECTORY"
    mv ./* core
    zip -q -r "$ARCHIVE_FILE" .
fi

if [ -f "$ARCHIVE_FILE" ]; then
    echo "Found test-report archive. Uploading it to 'transfer.sh'."

    TRANSFER_SH_URL=$(curl -s --upload-file "$ARCHIVE_FILE" "https://transfer.sh/$ARCHIVE_FILE_NAME")

    echo "Archive containing test-reports can be found and downloaded at the following URL:"
    echo ""
    echo "        $TRANSFER_SH_URL"
    echo ""
else
    echo "Archive containing test-reports does not exist. Nothing to upload."
fi