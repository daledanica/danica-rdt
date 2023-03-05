package com.danicadale.cs372.rdt;



import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;

public class UnreliableChannel
{
    /* ************************************************************************************************************** */
    /* Class Scope Variables                                                                                          */
    /*                                                                                                                */
    /*                                                                                                                */
    /*                                                                                                                */
    /*                                                                                                                */
    /* ************************************************************************************************************** */
    public static final double RATIO_DROPPED_PACKETS = 0.1;
    public static final double RATIO_DELAYED_PACKETS = 0.1;
    public static final double RATIO_DATA_ERROR_PACKETS = 0.1;
    public static final double RATIO_OUT_OF_ORDER_PACKETS = 0.1;
    public static final double ITERATIONS_TO_DELAY_PACKETS = 5;

    private ArrayList<Segment> sendQueue = new ArrayList<>();
    private ArrayList<Segment> receiveQueue = new ArrayList<>();
    private ArrayList<Segment> delayedPackets = new ArrayList<>();

    boolean canDeliverOutOfOrder;
    boolean canDropPackets;
    boolean canDelayPackets;
    boolean canHaveChecksumErrors;

    // Stats
    private int countTotalDataPackets = 0;
    private int countSentPackets = 0;
    private int countChecksumErrorPackets = 0;
    private int countDroppedPackets = 0;
    private int countDelayedPackets = 0;
    private int countOutOfOrderPackets = 0;
    private int countAckPackets = 0;
    private int currentIteration = 0;

    private String which; //###
    /* ************************************************************************************************************** */
    /* Constructors                                                                                                   */
    /*                                                                                                                */
    /*                                                                                                                */
    /*                                                                                                                */
    /*                                                                                                                */
    /* ************************************************************************************************************** */

    public UnreliableChannel( String which, //###
            boolean canDeliverOutOfOrder, boolean canDropPackets, boolean canDelayPackets, boolean canHaveChecksumErrors)
    {
        this.which = which + ": "; //###
        this.canDeliverOutOfOrder = canDeliverOutOfOrder;
        this.canDropPackets = canDropPackets;
        this.canDelayPackets = canDelayPackets;
        this.canHaveChecksumErrors = canHaveChecksumErrors;
    }

    /* ************************************************************************************************************** */
    /* Getters                                                                                                        */
    /*                                                                                                                */
    /*                                                                                                                */
    /*                                                                                                                */
    /*                                                                                                                */
    /* ************************************************************************************************************** */
    public int getCountTotalDataPackets() {
        return countTotalDataPackets;
    }

    public int getCountSentPackets() {
        return countSentPackets;
    }

    public int getCountChecksumErrorPackets() {
        return countChecksumErrorPackets;
    }

    public int getCountDroppedPackets() {
        return countDroppedPackets;
    }

    public int getCountDelayedPackets() {
        return countDelayedPackets;
    }

    public int getCountOutOfOrderPackets() {
        return countOutOfOrderPackets;
    }

    public int getCountAckPackets() {
        return countAckPackets;
    }

    public int getCurrentIteration() {
        return currentIteration;
    }

    public void send(Segment seg)
    {
        System.out.println(which + "### send(" + seg.to_string() + "): BEGIN");
        this.sendQueue.add(seg);
        System.out.println(which + "### send(" + seg.to_string() + "): END");
    }

    /* ************************************************************************************************************** */
    /* Public Functions                                                                                               */
    /*                                                                                                                */
    /*                                                                                                                */
    /*                                                                                                                */
    /*                                                                                                                */
    /* ************************************************************************************************************** */

    public ArrayList<Segment> receive()
    {
        // ### Creating an empty ArrayList violates the contract of Collecetion.copy(): "The destination list's size
        // must be greater than or equal to the source list's size".  Had to fix by init'ing the size of the
        // ArrayList to be the size of the receiveQueue
        ArrayList<Segment> new_list = new ArrayList<>(this.receiveQueue.size());
        new_list.addAll(this.receiveQueue);
        //Collections.copy(new_list, this.receiveQueue);
        this.receiveQueue.clear();
        System.out.println(which + "===receive() begin===");
        System.out.println(which + "UnreliableChannel len receiveQueue: " + this.receiveQueue.size());

        // ###
        for (Segment segment : this.receiveQueue) {
            System.out.println(which + "UnreliableChannel receiveQueue segment: " + segment.to_string());
        }
        System.out.println(which + "===receive() end===");

        return new_list;
    }

    public void processData()
    {
        System.out.println(which + "### UnreliableChannel.processData(): BEGIN");
        System.out.println(which + "UnreliableChannel manage - len sendQueue: " + this.sendQueue.size());
        // ###
        for (Segment segment : this.sendQueue) {
            System.out.println(which + "### UnreliableChannel.processData():    sendQueue: " + segment.to_string());
        }
        this.currentIteration++;
        System.out.println(which + "### UnreliableChannel.processData():    this.currentIteration: " + this.currentIteration);
        System.out.println(which + "### UnreliableChannel.processData():    this.sendQueue.size(): " + this.sendQueue.size());
        if ( this.sendQueue.size() == 0)
        {
            System.out.println(which + "### UnreliableChannel.processData(): END: nothing to send!");
            return;
        }

        System.out.println(which + "### UnreliableChannel.processData(): canDeliverOutOfOrder");
        if (this.canDeliverOutOfOrder )
        {
            Random random = new Random();
            float val = random.nextFloat();
            if ( val <= UnreliableChannel.RATIO_OUT_OF_ORDER_PACKETS )
            {
                this.countOutOfOrderPackets++;
                Collections.reverse(this.sendQueue);
            }
        }

        // add in delayed packets
        System.out.println(which + "### UnreliableChannel.processData(): delayedPackets...");
        ArrayList<Segment> noLongerDelayed = new ArrayList<>();
        for ( Segment seg : this.delayedPackets )
        {
            int numIterDelayed = this.currentIteration - seg.getStartIteration();

            if (numIterDelayed >= UnreliableChannel.ITERATIONS_TO_DELAY_PACKETS )
            {
                noLongerDelayed.add(seg);
            }
        }

        System.out.println(which + "### UnreliableChannel.processData(): noLongerDelayed...");
        for ( Segment seg : noLongerDelayed )
        {
            this.countSentPackets++;
            this.delayedPackets.remove(seg);
            this.receiveQueue.add(seg);
        }

        for ( Segment seg : this.sendQueue )
        {
            // this.receiveQueue.add(seg);

            boolean addToReceiveQueue = false;

            if ( this.canDelayPackets )
            {
                Random random = new Random();
                double val = random.nextDouble();

                if ( val <= UnreliableChannel.RATIO_DELAYED_PACKETS )
                {
                    this.countDelayedPackets++;
                    seg.setStartDelayIteration(this.currentIteration);
                    this.delayedPackets.add(seg);
                    continue;
                }
            }

            if ( this.canDropPackets )
            {
                Random random = new Random();
                double val = random.nextDouble();

                if (val <= UnreliableChannel.RATIO_DROPPED_PACKETS )
                {
                    this.countDroppedPackets++;
                }
                else
                {
                    addToReceiveQueue = true;
                }
            }
            else
            {
                addToReceiveQueue = true;
            }

            if ( addToReceiveQueue )
            {
                this.receiveQueue.add(seg);
                this.countSentPackets++;
            }

            if ( seg.getAckNumber() == -1 )
            {
                this.countTotalDataPackets++;

                // only data packets can have checksum errors...
                if ( this.canHaveChecksumErrors )
                {
                    Random random = new Random();
                    double val = random.nextDouble();

                    if ( val <= UnreliableChannel.RATIO_DATA_ERROR_PACKETS )
                    {
                        seg.createChecksumError();
                        this.countChecksumErrorPackets++;
                    }
                }
            }
            else
            {
                // count ack packets...
                this.countAckPackets++;
            }

            System.out.println(which + "UnreliableChannel len receiveQueue: " + this.receiveQueue.size());
            // ###
            for (Segment segment : this.receiveQueue) {
                System.out.println(which + "### UnreliableChannel.processData(): receiveQueue segment (really rcvd after filters): " + segment.to_string());
            }
        }

        System.out.println(which + "### UnreliableChannel.processData(): clear the sendQueue");
        this.sendQueue.clear();
        System.out.println(which + "### UnreliableChannel.processData(): Post this.sendQueue.clear()");
        System.out.println(which + "UnreliableChannel manage - len receiveQueue: " + this.receiveQueue.size());
        // ###
        for (Segment segment : this.receiveQueue) {
            System.out.println(which + "### UnreliableChannel.processData(): receiveQueue segment (after sendQueue.clear): " + segment.to_string());
        }
        System.out.println("### UnreliableChannel.processData(): END");
    }
}
