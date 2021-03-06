[![Continuous Integration](https://github.com/tools4j/elara/workflows/Continuous%20Integration/badge.svg)](https://github.com/tools4j/elara/actions?query=workflow%3A%22Continuous+Integration%22)
[![Maven Central](https://img.shields.io/maven-central/v/org.tools4j/elara-core.svg)](https://search.maven.org/search?q=a:elara-*)
[![Javadocs](http://www.javadoc.io/badge/org.tools4j/elara-core.svg)](http://www.javadoc.io/doc/org.tools4j/elara-core)
## elara
The tools4j elara project provides an efficient, zero garbage framework to implement event sourcing applications.  

The event store is pluggable; a default implementation is available for [chronicle queue](https://github.com/OpenHFT/Chronicle-Queue).
  
Elara uses [direct buffers](https://www.javadoc.io/static/org.agrona/agrona/1.7.1/index.html?org/agrona/DirectBuffer.html) as defined by the [agrona](https://github.com/real-logic/agrona) library.  For this reason elara applications are perfectly suited to integrate with [SBE](https://github.com/real-logic/simple-binary-encoding) and [aeron](https://github.com/real-logic/aeron) UDP/IPC messaging.

The elara library is used productively in applications in the financial industry.  However elara is also under active development and some new plugins and features may be considered experimental especially if they are not released yet.

### Overview

#### Introduction
There are excellent introductions to event sourcing out there.  Some of our favorite links are
* https://microservices.io/patterns/data/event-sourcing.html
* https://www.youtube.com/watch?v=fhZwzm-d9ys
* https://martinfowler.com/eaaDev/EventSourcing.html

#### Elara Event Sourcing
![Elara Event Sourcing](./elara.jpg)

#### Terminology 
* **Command:** essentially an input message but enriched with timestamp, source and sequence number; can be a state modifying command or a query
* **Event:** result of processing a command; instruction how to modify state or what output to generate
* **Command Log:** persisted log that sequentially stores all incoming commands
* **Event Log:** persisted event log that sequentially stores all routed events
* **Application State:** transient in-memory state of the application;  can be constructed from events via _Event Applier_
* **Input:** a source of input commands, such as a message subscription
* **Output:** transforms selected events into output messages and publishes them to downstream applications
* **Command Processor:** handles command messages and has read-only access to application state; routes events
* **Event Applier:** triggered by events (routed or replayed); modifies the application state according to the event instruction

### Plugins
Plugins are optional features that can be configured when defining the application context.  Third party plugins can be
provided by implementing the plugin API.  

Plugins can define their own state and applications can optionally access plugin state when defining the plugins in the
elara context.  By convention plugins define commands and events with negative types which marks them as 'admin' types 
so that they can be easily distinguished from non-negative application types.

Elara provides the following default plugins:

* **base:** a plugin that is implicitly added and defines the base state required for all elara applications
* **boot:** a plugin that issues commands and events indicating that an elara application has been started and initialised
* **timer:** a plugin that allows routing of events to start and stop simple and periodic timers;  the plugin injects
             commands to fire or expire timers which are then removed from the timer state through an event 
             (see samples section for examples) 
* **metrics:** plugin to capture time and frequency metrics of the running application;  a running application 
               efficiently captures metrics in a message log file that can be inspected with the log printer tool  
* **replication:** *[experimental]* plugin to replicate events from a leader elara instance to follower instances that 
                   are applying events but do not process commands;  follower instances have identical state as the 
                   leader (after applying all events and assuming deterministic application logic);  together with the
                   provided leader change commands applications can use this plugin to support rolling upgrades or 
                   implement manual or automatic fail-over strategies
 

### Samples

#### Banking application
A simple banking app that supports the following commands:
* Creation of a bank account
* Money deposit, withdrawal and transfer

Sample code and test to run:
* [bank sample](https://github.com/tools4j/elara/tree/master/elara-samples/src/main/java/org/tools4j/elara/samples/bank)
* [bank test](https://github.com/tools4j/elara/blob/master/elara-samples/src/test/java/org/tools4j/elara/samples/bank/BankApplicationTest.java)

#### Timers
Timers are tricky with event sourcing.  Elara provides timers through the [timer plugin](https://github.com/tools4j/elara/tree/master/elara-core/src/main/java/org/tools4j/elara/plugin/timer) with support for once-off and periodic timers.  The timer sample app demonstrates both timer types in action:
* [timer app](https://github.com/tools4j/elara/tree/master/elara-samples/src/main/java/org/tools4j/elara/samples/timer)
* [timer app test](https://github.com/tools4j/elara/blob/master/elara-samples/src/test/java/org/tools4j/elara/samples/timer/TimerApplicationTest.java)

### Maven/Gradle

#### Maven
```xml
<dependency>
        <groupId>org.tools4j</groupId>
        <artifactId>elara-core</artifactId>
        <version>1.7</version>
</dependency>
```

#### Gradle
```
api "org.tools4j:elara-core:1.7'
```

### Download
You can download binaries, sources and javadoc from maven central:
* [elara download](https://search.maven.org/search?q=a:elara-*)
