<a href="https://github.com/Pupsen-Vupsen/trik-testsys-grading-system/actions"><img alt="Build Status" src="https://github.com/Pupsen-Vupsen/trik-testsys-grading-system/actions/workflows/build.yml/badge.svg"></a>
<a href="https://github.com/Pupsen-Vupsen/trik-testsys-grading-system/actions"><img alt="Test Status" src="https://github.com/Pupsen-Vupsen/trik-testsys-grading-system/actions/workflows/test.yml/badge.svg"></a>
![GitHub](https://img.shields.io/github/license/Pupsen-Vupsen/trik-testsys-grading-system?color=blue&logo=apache)
[![CodeFactor](https://www.codefactor.io/repository/github/pupsen-vupsen/trik-testsys-grading-system/badge)](https://www.codefactor.io/repository/github/pupsen-vupsen/trik-testsys-grading-system)

# Server part for TRIK Studio grading system

Current Swagger documentation to API is available [here](https://app.swaggerhub.com/apis/5h15h4k1n9/trik-testsys-grading-system/2.0.6).

## Overview 

Grading system server part for TRIK Studio, which allows you to test you task using TRIK Studio engine.

## How to run locally

1. Copy this repository to your local machine.
2. Install Java version 11 or later.
3. Move to the root of the project.
4. Run `./gradlew :bootJar`.
5. Run `java -jar ./build/libs/trik-testsys-grading-system-<version>.jar`.

## How to run via Docker

1. Install Docker to your local machine.
2. Run `docker pull 5h15h4k1n9/trik-testsys-grading-system`.
3. Run `docker run -it -p 8080:8080 5h15h4k1n9/trik-testsys-grading-system`.
