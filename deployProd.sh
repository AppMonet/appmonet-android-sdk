#!/bin/bash

./gradlew clean
./gradlew bintrayUpload -Penv=prod
./gradlew s3Upload
