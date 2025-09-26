# aurenworks-api

This project uses Quarkus, the Supersonic Subatomic Java Framework.

If you want to learn more about Quarkus, please visit its website: <https://quarkus.io/>.

## Running the application in dev mode

You can run your application in dev mode that enables live coding using:

```shell script
./mvnw quarkus:dev
```

> **_NOTE:_**  Quarkus now ships with a Dev UI, which is available in dev mode only at <http://localhost:8080/q/dev/>.

## Packaging and running the application

The application can be packaged using:

```shell script
./mvnw package
```

It produces the `quarkus-run.jar` file in the `target/quarkus-app/` directory.
Be aware that it’s not an _über-jar_ as the dependencies are copied into the `target/quarkus-app/lib/` directory.

The application is now runnable using `java -jar target/quarkus-app/quarkus-run.jar`.

If you want to build an _über-jar_, execute the following command:

```shell script
./mvnw package -Dquarkus.package.jar.type=uber-jar
```

The application, packaged as an _über-jar_, is now runnable using `java -jar target/*-runner.jar`.

## Creating a native executable

You can create a native executable using:

```shell script
./mvnw package -Dnative
```

Or, if you don't have GraalVM installed, you can run the native executable build in a container using:

```shell script
./mvnw package -Dnative -Dquarkus.native.container-build=true
```

You can then execute your native executable with: `./target/aurenworks-api-1.0.0-SNAPSHOT-runner`

If you want to learn more about building native executables, please consult <https://quarkus.io/guides/maven-tooling>.

## Code Formatting with Spotless

This project uses [Spotless](https://github.com/diffplug/spotless) for code formatting and style enforcement. Spotless is configured to use the Eclipse formatter with 2-space indentation as specified in the project rules.

### Running Spotless

To check if your code is properly formatted:

```shell script
./mvnw spotless:check
```

To automatically format your code:

```shell script
./mvnw spotless:apply
```

### Spotless Configuration

- **Java files**: Formatted using Eclipse formatter with `eclipse-formatter.xml`
- **XML files**: Formatted using Eclipse WTP XML formatter
- **Import management**: Unused imports are automatically removed
- **Annotations**: Properly formatted
- **Line endings**: UNIX style (LF)
- **Encoding**: UTF-8

### Pre-commit Hook (Optional)

To automatically format code before commits, you can set up a pre-commit hook:

```shell script
# Create the pre-commit hook
echo '#!/bin/sh
./mvnw spotless:apply
git add .
' > .git/hooks/pre-commit

# Make it executable
chmod +x .git/hooks/pre-commit
```

### CI Integration

Spotless is configured to run during the `apply` phase of the Maven build, ensuring all code is properly formatted before compilation.

## Provided Code

### REST

Easily start your REST Web Services

[Related guide section...](https://quarkus.io/guides/getting-started-reactive#reactive-jax-rs-resources)
