# danica-rdt

This is my Reliable Data Transfer project for CS-372



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

