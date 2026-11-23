# quarkus-portfolio-rag-advisor

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

You can then execute your native executable with: `./target/quarkus-portfolio-rag-advisor-0.0.1-runner`

If you want to learn more about building native executables, please consult <https://quarkus.io/guides/maven-tooling>.

## Related Guides

- Qute ([guide](https://quarkus.io/guides/qute)): Offer templating support for web, email, etc in a build time, type-safe way
- REST ([guide](https://quarkus.io/guides/rest)): Build RESTful web services and APIs using Jakarta REST (formerly JAX-RS)
- REST Jackson ([guide](https://quarkus.io/guides/rest#json-serialisation)): Jackson serialization support for Quarkus REST. This extension is not compatible with the quarkus-resteasy extension, or any of the extensions that depend on it
- JDBC Driver - PostgreSQL ([guide](https://quarkus.io/guides/datasource)): Connect to the PostgreSQL database via JDBC

## Provided Code

### REST

Easily start your REST Web Services

[Related guide section...](https://quarkus.io/guides/getting-started-reactive#reactive-jax-rs-resources)

## Portfolio web interface

Open <http://localhost:8000/> after starting Quarkus. The Qute page and its CSS/JavaScript
are served by the same application; no frontend server or build is needed. Fonts use
Google Fonts when available, with local system fallbacks. The page contains no initial
profile or evidence data. Select an existing developer ID to fetch the declared base
stack from `GET /api/v1/developers/{id}`. Declared skills are not verified GitHub evidence.

### Local Advisor and privacy

The full Advisor form **and REST operation** are available only in the Quarkus `dev`
launch mode. Outside that mode, `/api/v1/developers/{id}/advisor` returns HTTP 403
before calling the analysis service. This is a demo restriction, not authentication.
Run a local demonstration with `./mvnw quarkus:dev -Dquarkus.http.host=127.0.0.1`;
do not expose dev mode through a public listener, tunnel, proxy or port forward.

The Advisor deliberately retains its internal retrieval behavior: evidence can include
private repositories and is sent to the configured AI provider. Aliases and missing
source URLs do **not** guarantee confidentiality. The generated answer, strengths and
tip text can contain private names, URLs, client information or internal identifiers.
Use only synthetic data or data you are authorized to send. Do not record or share a
session containing private evidence. Proper authorization or data isolation is required
before enabling a hosted Advisor. No credentials or AI calls are made directly by the
browser; it calls the existing backend. Model text is displayed as plain text.
