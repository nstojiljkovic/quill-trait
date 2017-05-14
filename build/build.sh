#!/bin/bash

set -e

sbt clean ++2.11.11 coverage test tut coverageReport coverageAggregate