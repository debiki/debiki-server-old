#!/bin/bash

if [ -z "$activator" ]; then
  activator=scripts/activator-1.2.3
fi

timeout_activator="$activator"
if [ "$1" = "--timeout" ]; then
  shift
  timeout_activator="scripts/timeout.sh -t $1 -d 10 $activator"
  shift
fi

$timeout_activator "$@"
if [ $? -ne 0 ]; then
  echo "$@" >> target/tests-failed
fi
