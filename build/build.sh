#!/bin/bash
set -e # Any subsequent(*) commands which fail will cause the shell script to exit immediately

sbt clean ++2.11.11 "quill-traitJVM/test"