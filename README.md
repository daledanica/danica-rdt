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

