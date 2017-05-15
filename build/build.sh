#!/bin/bash

set -e

sbt clean ++2.12.2 test ++2.11.11 coverage test tut coverageReport coverageAggregate