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

    /* ************************************************************************************************************** */
    /* Constructors                                                                                                   */
    /*                                                                                                                */
    /*                                                                                                                */
    /*                                                                                                                */
    /*                                                                                                                */
    /* ************************************************************************************************************** */

    public UnreliableChannel(boolean canDeliverOutOfOrder, boolean canDropPackets, boolean canDelayPackets, boolean canHaveChecksumErrors)
    {
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
        this.sendQueue.add(seg);
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
        // ### BUG: Creating an empty ArrayList violates the contract of Collecetion.copy(): "The destination list's size
        // must be greater than or equal to the source list's size".  Had to fix by init'ing the size of the
        // ArrayList to be the size of the receiveQueue
        ArrayList<Segment> new_list = new ArrayList<>(this.receiveQueue.size());
        new_list.addAll(this.receiveQueue);
        //Collections.copy(new_list, this.receiveQueue);
        this.receiveQueue.clear();
        System.out.println("===receive() begin===");
        System.out.println("UnreliableChannel len receiveQueue: " + this.receiveQueue.size());

        return new_list;
    }

    public void processData()
    {
        System.out.println("UnreliableChannel manage - len sendQueue: " + this.sendQueue.size());

        this.currentIteration++;

        if ( this.sendQueue.size() == 0)
        {
            return;
        }

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
        ArrayList<Segment> noLongerDelayed = new ArrayList<>();
        for ( Segment seg : this.delayedPackets )
        {
            int numIterDelayed = this.currentIteration - seg.getStartIteration();

            if (numIterDelayed >= UnreliableChannel.ITERATIONS_TO_DELAY_PACKETS )
            {
                noLongerDelayed.add(seg);
            }
        }

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

            System.out.println("UnreliableChannel len receiveQueue: " + this.receiveQueue.size());
        }

        this.sendQueue.clear();
        System.out.println("UnreliableChannel manage - len receiveQueue: " + this.receiveQueue.size());
    }
}
