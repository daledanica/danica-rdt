# danica-rdt

This is my Reliable Data Transfer project for CS-372


## Design Notes
This implementation uses Sequence Numbering mechanism (see Table 3.1, Chapter 3.4. <i>Computer Networking: a top down approach</i>, James Kurose and Keith Ross, pub. Pearson ebooks)
* Checksumming is used to ensure data integrity.
* Cumulative Acks are used to minimize ack traffic.
* Configurable timeouts and Selective Retransmit are used.
* Flow control windows are used so as not to overwhelm server-side resources.
* Transmit pipelining is used (within the constraints of flow control) to reduce chattiness and allow Cumulative Acks to be effective.

### Efficiency
A hard-limit of 15 data characters was imposed on the size of the flow control window.  Maximum efficiency in terms of minimal iterations was achieved by making the individual data segments that size.  About 300 iterations were needed to successfully transmit the message, depending on how many random errors were induced by the unreliable channel.

But, of course, using data segments the full size of the flow control window prevents pipelining.  Next best was to to use half the flow control window size, but that tending to make data segments of size 7, 7, and 1.  So, for the sake of experiment and symmetry, a data segment size of 5 was used.  It did indeed turn out to cost more iterations than 7, 7, 1, but it just felt nicer (not that nicer counts in the real world of computational and network costs).

## Bugs in supplied code
The following bugs in the supplied Java code were discovered and corrected:

* Fatal violation of Collections.copy() contract in <i>UnreliableChannel.java</i>:
```
public ArrayList<Segment> receive()
{
// ### BUG: Creating an empty ArrayList violates the contract of Collecetion.copy(): "The destination list's size
// must be greater than or equal to the source list's size".  Had to fix by init'ing the size of the
// ArrayList to be the size of the receiveQueue.  Then just us a simple allAll()
ArrayList<Segment> new_list = new ArrayList<>(this.receiveQueue.size());
new_list.addAll(this.receiveQueue);
//Collections.copy(new_list, this.receiveQueue);
this.receiveQueue.clear();
System.out.println("===receive() begin===");
System.out.println("UnreliableChannel len receiveQueue: " + this.receiveQueue.size());

      return new_list;
}
```
<br/> 
* Supplied source Java code was packageless. 
<br/>
<ul>
The following was added to the top of each supplied Java source file:

  `package com.danicadale.cs372.rdt;`
</ul>
<br/> 
* Missing getters for <code>Segment</code> data members:
<ul>
The following getter functions were added to <i>Segment.java</i>:

```
public String getPayload() { return this.payload; }
public int getSegmentNumber() { return this.seqmentNumber; }
```
</ul>
<br/>  

# Build and Run

## Ready to go jars
For your convenience, build/lib contains two jars; one that uses the "short" message ("the quick brown fox jumps over the lazy dog").  The other uses the "long" message (Kennedy's famous moon shot speech).
* They are built with Java 1.8.0_361.  
* Host "flip" has Java 1.8.0_362 and I've verified that it can run these jars, as such:
<br/>
  <code>% java -cp danica-rdt-long.jar com.danicadale.cs372.rdt.RDT_Main</code>
  
## Roll your own

This is a bit of a pain without an IDE (e.g., IntelliJ or Eclipse), but it can be done.
1. Create a directory to do this in
```
% mkdir dev
% cd dev
```
2. Create the directory structure that mirrors Java's package:
```
% mkdir -p com/danicadale/cs372/rdt
```
3. Copy the Java source files into the package directory such that it looks like this:
```aidl
% ls com/danicadale/cs372/rdt
com/danicadale/cs372/rdt/RDTLayer.java		com/danicadale/cs372/rdt/Segment.java
com/danicadale/cs372/rdt/RDT_Main.java		com/danicadale/cs372/rdt/UnreliableChannel.java
```
4. Compile
```
% javac com/danicadale/cs372/rdt/*.java
% ls com/danicadale/cs372/rdt/*.class
com/danicadale/cs372/rdt/RDTLayer$1.class		com/danicadale/cs372/rdt/RDT_Main.class
com/danicadale/cs372/rdt/RDTLayer$Message.class		com/danicadale/cs372/rdt/Segment.class
com/danicadale/cs372/rdt/RDTLayer$Packet.class		com/danicadale/cs372/rdt/UnreliableChannel.class
com/danicadale/cs372/rdt/RDTLayer.class
```
5. Jar it up
```
% jar cvf danica-rdt.jar com/danicadale/cs372/rdt/*.class
added manifest
adding: com/danicadale/cs372/rdt/RDTLayer$1.class(in = 12920) (out= 12093)(deflated 6%)
adding: com/danicadale/cs372/rdt/RDTLayer$Message.class(in = 4326) (out= 2031)(deflated 53%)
adding: com/danicadale/cs372/rdt/RDTLayer$Packet.class(in = 1380) (out= 735)(deflated 46%)
adding: com/danicadale/cs372/rdt/RDTLayer.class(in = 6014) (out= 2939)(deflated 51%)
adding: com/danicadale/cs372/rdt/RDT_Main.class(in = 4300) (out= 2209)(deflated 48%)
adding: com/danicadale/cs372/rdt/Segment.class(in = 2714) (out= 1342)(deflated 50%)
adding: com/danicadale/cs372/rdt/UnreliableChannel.class(in = 4466) (out= 2258)(deflated 49%)
% ls *.jar
danica-rdt.jar
```
6. Run it!
```
% java -cp danica-rdt.jar com.danicadale.cs372.rdt.RDT_Main
:
:
Main--------------------------------------------
DataReceivedFromClient: 'The quick brown fox jumped over the lazy dog'
$$$$$$$$ ALL DATA RECEIVED $$$$$$$$
countTotalDataPackets: 14
countSentPackets: 20
countChecksumErrorPackets: 2
countOutOfOrderPackets: 2
countDelayedPackets: 1
countDroppedDataPackets: 2
countAckPackets: 7
countDroppedAckPackets: 0
# segment timeouts: 6
TOTAL ITERATIONS: 14
```
Yay!
