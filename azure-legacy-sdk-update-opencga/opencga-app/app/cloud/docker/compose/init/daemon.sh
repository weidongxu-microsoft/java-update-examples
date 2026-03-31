#!/bin/sh -x

# Deploy new catalog
sleep 20
echo "Starting REST"
echo "<REDACTED>" | ./bin/opencga-admin.sh catalog daemon --start

