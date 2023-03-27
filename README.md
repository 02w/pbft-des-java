# `pbft-java-des`

A Practical Byzantine Fault Tolerance (PBFT) emulator built in Java for Discrete Event Simulation.

Forked from [caojohnny/pbft-java](https://github.com/caojohnny/pbft-java).

# Implementation

For the most part, the implementation of the PBFT protocol
attempts to follow the original document as closely as
possible. However, there are many places where I am simply
too stupid to comprehend what is going on in the protocol,
and there may be a few deviations from the standard.

In the default implementations, clients are allowed to send
asynchronous requests. Clients implement a ticketing system
whereby requests are assigned a ticket and placed into an
internal table, which will allow lookups based on the
timestamp of the original request. Because requests may be
sent quickly, timestamps begin at 0 and count up instead of
using the system clock for simplicity. Clients check on the
reply status of a ticket in an infinite loop, as long as
timeouts continue to occur, clients will continue to
multicast the same request to all replicas. Clients accept
a quorum of `f + 1` replies, and future replies from the
remaining nodes are ignored.

In the default implementations, replicas are allowed to
handle asynchronous requests. The message logging style
also uses a ticketing system, whereby operations that
are pending are identified by view number and the assigned
sequence number. Tickets are phased rather than the entire
replica. All messages received as well as sent are added to
the respective ticket. Replicas check for prepared state as
well as committed-local state each time a phase-pertinent
message is sent (`PRE-PREPARE`, `PREPARE`, or `COMMIT`),
and thus these messages are allowed to arrive out-of-order.
Replicas execute all requested operations synchronously.
Replicas are allowed to send a `PREPARE` or `COMMIT`
message only once to cut down on traffic. Cryptography,
such as digesting, MACs, and message signing are not
specified, and implementors are allowed to not verify those
if desired. Replicas become prepared as soon as the
`2f`th matching `PREPARE` message arrives, and become 
committed-local as soon as the `2f + 1`th matching 
`COMMIT` arrives. Because these conditions only occur once,
future phase messages are ignored if the condition they are
changing are already true.

# Building

``` shell
git clone https://github.com/02w/pbft-des-java.git
cd pbft-des-java
mvn clean install
```

# Usage

The majority of *response* logic has been implemented by
the `Default*` implementation modules. This means that
given input, the default implementations handles the
response to those inputs. The user does need  to implement
a few components in order to correctly use the provided
implementations.

Sample implementations for the required components can
be found the `pbft-java-example` module.

#### Clients

- Clients need to implement their own `ClientEncoder` to
transform the messages into a transmissible format
    - Encoders handle message signing and MACs
- Clients also need to implement the `ClientTransport` in
order for the `Client` to send messages
- Clients need to implement their own incoming message
handlers that both decode the message and decide which
hooks to call - the required hook is 
`Client#recvReply(...)`
- Client users should call `Client#checkTimeout(...)` in
a loop after sending requests in order to ensure liveness
- Operations implemented by the client should implement
`equals` and `hashCode`

#### Replicas

- Replicas need to implement their own `ReplicaEncoder` to
transform the messages into a transmissible format
    - Encoders handle message signing and MACs
    - The **default** implementation requires that full
    messages and checkpoints are encoded for those messages
    pertaining to view changes, but this can be changed if
    the user wishes to accommodate for additional messages
    to retrieve missing information
- Replicas need to implement `ReplicaTransport` in order
for the `Replica` to send messages
- Replicas need to implement their own incoming message
handlers that both decode and decide which hooks to call -
the required hooks are:
  - `#recvRequest(...)`
  - `#recvPrePrepare(...)`
  - `#recvPrepare(...)`
  - `#recvCommit(...)`
  - `#recvCheckpoint(...)`
  - `#recvViewChange(...)`
  - `#recvNewView(...)`
- Replicas need to implement their own `Digesters` if
needed
- Replicas need to call `#checkTimeout(...)` in a loop to
ensure that client timeouts cause view changes as needed

# Demo

Check `pbft-java-example-des` module.

# Credits

Built with [IntelliJ IDEA](https://www.jetbrains.com/idea/)


# References

  - [Practical Byzantine Fault Tolerance](http://pmg.csail.mit.edu/papers/osdi99.pdf)
  - [Practical BFT](https://courses.cs.washington.edu/courses/csep552/13sp/lectures/10/pbft.pdf)
  - [Practical Byzantine Fault Tolerance](http://www.scs.stanford.edu/14au-cs244b/notes/pbft.txt)
  - [Distributed Algorithms Practical Byzantine Fault Tolerance](https://disi.unitn.it/~montreso/ds/handouts17/10-pbft.pdf)
  - [PBFT Presentation](https://courses.cs.vt.edu/~cs5204/fall05-gback/presentations/PBFT.pdf)
  - [Practical Byzantine Fault Tolerance](https://www.microsoft.com/en-us/research/wp-content/uploads/2017/01/thesis-mcastro.pdf)
  - [10.BFT](https://www.cs.utexas.edu/~lorenzo/corsi/cs380d/past/10S/notes/week11.pdf)
  - [Byzantine Fault Tolerance](http://www.cs.cmu.edu/~srini/15-440-all/2016.Fall/lectures/22-BFT.ppt)
  - [Practical Byzantine Fault Tolerance ](https://people.eecs.berkeley.edu/~kubitron/courses/cs294-4-F03/slides/lec09-practical.ppt)
