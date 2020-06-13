[![Continuous Integration](https://github.com/tools4j/elara/workflows/Continuous%20Integration/badge.svg)](https://github.com/tools4j/elara/actions?query=workflow%3A%22Continuous+Integration%22)
[![Maven Central](https://img.shields.io/maven-central/v/org.tools4j/elara-core.svg)](https://search.maven.org/search?q=a:elara-*)
[![Javadocs](http://www.javadoc.io/badge/org.tools4j/elara-core.svg)](http://www.javadoc.io/doc/org.tools4j/elara-core)
## elara
The tools4j elara project provides an efficient, zero garbage framework to implement event sourcing applications.  

  The event store is pluggable; a default implementation is available for [chronicle queue](https://github.com/OpenHFT/Chronicle-Queue).
  
#### Introduction 

#### Samples

###### Banking application
A simple banking app that supports the following commands:
* Creation of a bank account
* Money deposit, withdrawal and transfer

Sample code and test to run:
* [bank sample](https://github.com/tools4j/elara/tree/master/elara-samples/src/main/java/org/tools4j/elara/samples/bank)
* [bank test](https://github.com/tools4j/elara/blob/master/elara-samples/src/test/java/org/tools4j/elara/samples/bank/BankApplicationTest.java)

###### Timers
Timers are tricky with event sourcing.  Elara provides timers through the [timer plugin](TODO) with support for once-off and periodic timers.  The timer sample app demostrates both timer types in action:
* [timer app](https://github.com/tools4j/elara/tree/master/elara-samples/src/main/java/org/tools4j/elara/samples/timer)
* [timer app test](https://github.com/tools4j/elara/blob/master/elara-samples/src/test/java/org/tools4j/elara/samples/timer/TimerApplicationTest.java)

#### Maven/Gradle

###### Maven
```xml
<dependency>
        <groupId>org.tools4j</groupId>
        <artifactId>elara-core</artifactId>
        <version>1.2</version>
</dependency>
```

###### Gradle
```
api "org.tools4j:elara-core:1.2'
```

#### Download
You can download binaries, sources and javadoc from maven central:
* [elara download](https://search.maven.org/search?q=a:elara-*)
