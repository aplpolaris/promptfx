# promptfx

This project provides a Kotlin library for AI prompt chaining (`promptkt`) and an associated JavaFx demonstration UI (`promptfx`).

## PromptFx UI Features

TBD

## PromptKt Library Features

TBD

## Building PromptKt and PromptFx

System requirements:
- Maven version 3.9.3+
- Java version 17+

TornadoFx library:
- Uses a version of `tornadofx` that is not available on maven central.
- Clone the repo https://github.com/triathematician/tornadofx.
- Checkout out the `jdk11-fx18-kotlin16` branch.
- Run `mvn clean install` to install the `tornadofx` library in your local maven repository.

To build the project:
- Run `mvn clean install -DskipTests` from the `promptkt` directory.
- Run `mvn clean install -DskipTests` from the `promptfx` directory.

Note that you can run tests as part of the build, but many of these require an `apikey.txt` file and use the OpenAI API. We anticipate migrating these to a separate profile and making them available as optional integration tests.

## Running PromptFx

To run the project (after compilation):
- Run `java -jar target/promptfx-0.1.0-SNAPSHOT-jar-with-dependencies.jar` from the `promptfx` directory.
