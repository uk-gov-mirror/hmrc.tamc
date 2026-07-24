tamc
====

Allows users to create, update, and retrieve Marriage Allowance relationships over supported tax years.

Requirements
------------

This service is written in [Scala 3.x](http://www.scala-lang.org/) and [Play 3.x](http://playframework.com/), so needs at least a [JRE 21](http://www.oracle.com/technetwork/java/javase/downloads/index.html) to run.

API
---

| *Task* | *Supported Methods* | *Description* | Status |
|--------|----------------------|---------------|--------|
| `/paye/:transferorNino/list-relationship` | GET | Retrieves existing Marriage Allowance relationship(s) for the transferor NINO. | Live |
| `/paye/:transferorNino/get-recipient-relationship` | POST | Retrieves a recipient relationship for the provided transferor NINO. | Live |
| `/paye/:transferorNino/create-multi-year-relationship/:journey` | PUT | Creates a multi-year Marriage Allowance relationship for a journey type. | Live |
| `/paye/:transferorNino/update-relationship` | PUT | Updates an existing Marriage Allowance relationship. | Live |

Configuration
-------------

All downstream services require host and port settings, for example:

| *Key* | *Description* |
|-------|---------------|
| `microservice.services.auth.host` | Host of the Auth service |
| `microservice.services.auth.port` | Port of the Auth service |
| `microservice.services.email.host` | Host of the Email service |
| `microservice.services.email.port` | Port of the Email service |
| `microservice.services.marriage-allowance-des.host` | Host of the Marriage Allowance DES service |
| `microservice.services.marriage-allowance-des.port` | Port of the Marriage Allowance DES service |
| `microservice.services.pertax.host` | Host of the Pertax service |
| `microservice.services.pertax.port` | Port of the Pertax service |

How to test the project
=======================

Unit tests
----------
- **Unit test the entire test suite:** `sbt test`
- **Unit test a single spec file:** `sbt "testOnly *fileName"` (for example: `sbt "testOnly *MarriageAllowanceControllerSpec"`)

Integration tests
-----------------
- **Run integration tests:** `sbt it/test`

Acceptance tests
----------------
Acceptance tests are maintained in the [tamc-acceptance-tests](https://github.com/hmrc/tamc-acceptance-tests) repository.

Acronyms
--------

In the context of this service we use the following acronyms:

* NINO: National Insurance Number
* DES: Data Exchange Service
* API: Application Programming Interface
* JRE: Java Runtime Environment
* JSON: JavaScript Object Notation

License
-------

This code is open source software licensed under the Apache 2.0 License.
