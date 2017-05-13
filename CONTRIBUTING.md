# How to Contribute

Instructions on how to contribute to Quill-trait project. Highly inspired by the Quill "core".

## Building the project

The only dependency you need to build Quill locally is [Docker](https://www.docker.com/).

If you are running Linux, you should also install Docker Compose separately, as described
[here](https://docs.docker.com/compose/install/).

After installing Docker and Docker Compose, you have to run the command bellow in
order to setup the databases' schemas. If you don't change any schemas, you will
only need to do this once.

```
docker-compose run --rm setup
```

After that, just run the command bellow to build and test the project.

```
docker-compose run --rm sbt sbt "quill-traitJVM/test"
```

## Changing database schema

If you have changed any file that creates a database schema, you will
have to setup the databases again. To do this, just run the command bellow.

```
docker-compose stop && docker-compose rm && docker-compose run --rm setup
```

## Tests

### Running tests

Run all tests:
```
docker-compose run --rm sbt sbt "quill-traitJVM/test"
```

Run specific test:
```
docker-compose run --rm sbt sbt "project quill-traitJVM" "test-only com.nikolastojiljkovic.quill.QueryMetaSpec"
```

### Debugging tests
1. Run sbt in interactive mode with docker container ports mapped to the host: 
```
docker-compose run --service-ports --rm sbt
```

2. Attach debugger to port 15005 of your docker host. In IntelliJ IDEA you should create Remote Run/Debug Configuration, 
change it port to 15005.

3. In sbt command line run tests with `test` or test specific spec by passing full name to `test-only`:
```
> quill-traitJVM/test-only com.nikolastojiljkovic.quill.QueryMetaSpec
```
