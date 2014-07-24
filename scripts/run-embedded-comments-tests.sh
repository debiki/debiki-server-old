#!/bin/bash

# Call this script e.g. like so:
# scripts/run-embedded-comments-tests.sh [timeout] -Dtestserver.port=19003  -Dtest.e2e.chrome.driverPath=/mnt/tmp/dev/chromedriver
# The 'timeout' flag causes the script to abort (kill its process group) if the tests take too long.

if [ -z "$activator" ]; then
  export activator=scripts/activator-1.2.3
fi

if [ "$1" = "timeout" ]; then
  shift
  timeout="timeout"
fi

timeout_activator="$activator"
function set_timeout {
  if [ -n "$timeout" ]; then
    timeout_activator="scripts/timeout.sh -t $1 -d 10 $activator"
  fi
}

# Testing embedded comments requires another server running on port 8080 that serves
# embedding pages. Start such a server:
pushd .
cd test/resources/embedding-pages/
http-server &
popd

set_timeout 500
$timeout_activator  "$@"  "test-only test.e2e.EndToEndSuiteForEmbeddedComments"
if [ $? -ne 0 ]; then
  echo "$@" >> target/tests-failed
fi

# Kill the embedding pages server. Apparently, since we've started a new Bash shell,
# the embedded server always gets id 1.
kill %1

