package com.danicadale.cs372.rdt;



import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;



/**
 * The reliable data transfer (RDT) layer is used to provide reliable communication over an unreliable channel
 *
 * @author Danica Dale   CS-372
 * @since March 2023
 */
public class RDTLayer {


    // Max length of the string data that will be sent per packet (in chars, not bytes)
    private static final int DATA_LENGTH = 4;

    // Max amount of data to send in a flow control window (in chars, not bytes)
    private static final int FLOW_CONTROL_WIN_SIZE = 15;

    // set this true to enable window-based flow control
    private static final boolean DO_FLOW_CONTROL = true;

    private static final int lastCumulativeAck = -1;

    // any sent data not ACKed in this many iterations is a timeout->resend
    private static final int TIME_OUT_ITERATIONS = 3;

    // turn on to enable diagnostic logging
    private static boolean DEBUG = false;

    // set this true to implement cumulative ACKing
    private final boolean CUMULATIVE_ACK = true;

    private final Message dataSent = new Message();  // used by client

    private final Message dataRcvd = new Message();  // used by server

    // how much room the server _should_ currently have available to receive data from the cilent
    private int currentWindowCapacity = FLOW_CONTROL_WIN_SIZE;

    // effectively, the number of time we have to resend a segment because we didn't get an ACK for it
    private int countSegmentTimeouts = 0;

    private UnreliableChannel sendChannel = null;

    private UnreliableChannel receiveChannel = null;

    private String dataToSend = "";

    // used as our network clock
    private int currentIteration = 0;

    // sequence counter
    private int currentSegmentNumber = 0;



    /**
     * Construct this RDT Layer
     */
    public RDTLayer() {

    }



    /**
     * Set the unreliable channel to be used for sending data.
     * <p>
     * This is set by main.
     *
     * @param channel The unreliable channel.
     */
    public void setSendChannel(UnreliableChannel channel) {

        this.sendChannel = channel;
    }



    /**
     * Set the unreliable channel to be used for receiving data.
     * <p>
     * This is set by main.
     *
     * @param channel The unreliable channel.
     */
    public void setReceiveChannel(UnreliableChannel channel) {

        this.receiveChannel = channel;
    }



    /**
     * Set the data this RDT layer is to send to the server.
     * <p>
     * This is set by main.
     *
     * @param data The data to send.
     */
    public void setDataToSend(String data) {

        this.dataToSend = data;
    }



    /**
     * Called by main to get the currently (as of now) received message data, in sequence order of segments.
     *
     * @return Called by main to get the currently (as of now) received message data, in sequence order of segments.
     *         Note that dropped, delayed, and corrupted segment will show up as human-noticeable errors (missing data).
     *         These will clear up as re-sends catch up with data issues.
     */
    public String getDataReceived() {

        return this.dataRcvd.getAllData();
    }



    /**
     * Get the number of segment ACK timeouts detected by the client.
     * <p>
     * Called by main.
     *
     * @return the number of segment ACK timeouts detected by the client.
     */
    public int getCountSegmentTimeouts() {

        return this.countSegmentTimeouts;
    }



    /**
     * Process all the incoming data, whether we're a server receiving data segments, or a client sending data segments
     * and receiving ACKs.
     * <p>
     * Called by main
     */
    public void processData() {

        this.currentIteration++;

        // first, deal with anything that's been received.
        processReceiveAndSendResponse();

        // now that we've dealt with any ACKs that have come in...
        processTimeouts();

        // then, do any sending
        processSend();
    }



    private void processSend() {

        // if we don't have anything to send, then:
        // -- We are either the server (which only sends ACKs, not data)
        // OR
        // -- We're the client and we've sent all of the entire message (but we may well still be waiting on
        //    ACKs and dealing with resends).
        //
        while (!this.dataToSend.isEmpty() && (this.currentWindowCapacity > 0)) {

            // if the flow control window says the server may not have resources to receive any new data, then we
            // need to wait for some ACKs or timeouts to free up some room
            while (this.currentWindowCapacity > 0) {

                // calculate how many chars of data we can send
                int len = Math.min(this.currentWindowCapacity, Math.min(this.dataToSend.length(), DATA_LENGTH));
                if (len <= 0) {
                    // nothing to send; bail out
                    return;
                }

                // carve off a slice of data from the leading portion of the remaining data we have to send
                String data = this.dataToSend.substring(0, len);
                if (len == this.dataToSend.length()) {
                    // nothing more to send
                    this.dataToSend = "";
                }
                else {
                    // data is not the remainder after the slice
                    this.dataToSend = this.dataToSend.substring(len);
                }

                // send the slice of data
                sendData(getCurrentSequenceNumber(), data);
            }
        }
    }



    private void processReceiveAndSendResponse() {

        // get the incoming segments that have been received and need to be processed
        ArrayList<Segment> listIncomingSegments = this.receiveChannel.receive();

        if (DEBUG) System.out.println("processReceiveAndSendRespond() received: "
                                      + listIncomingSegments.size()
                                      + " segments");
        if (listIncomingSegments.isEmpty()) {
            if (DEBUG) System.out.println("processReceiveAndSendRespond() nothing received; bailing out");
            return;
        }

        if (DEBUG) {
            for (Segment segment : listIncomingSegments) {
                System.out.println("processReceiveAndSendRespond() received:     " + segment.to_string());
            }
        }


        boolean anyDataRcvd = false;
        for (Segment segmentRcvd : listIncomingSegments) {

            int highestSequenceRcvd = -1;

            if (isData(segmentRcvd)) {
                // we should only be here if we're a server receiving from a client

                if (!segmentRcvd.checkChecksum()) {
                    System.out.println("processReceiveAndSendRespond():    bad checksum!!! on "
                                       + segmentRcvd.to_string());
                    // bail out; don't ACK.  The client will retransmit when it times out on not getting the ACK
                    continue;
                }

                // remember we've received this message segment, so we can put it together with all the outher
                // segments to form the final complete message
                this.dataRcvd.addPacket(segmentRcvd);
                anyDataRcvd = true;

                // ack the rcvd segment
                if (!CUMULATIVE_ACK) {
                    // we're ACKing every segment received from the client; very chatty
                    sendAck(segmentRcvd.getSegmentNumber());
                }
            }

            else if (isAck(segmentRcvd)) {
                // we should only be here if we're a client (servers don't rcv ACKs)
                Segment ack = segmentRcvd;
                if (DEBUG) System.out.println("processReceiveAndSendRespond():    processing ACK for "
                                              + ack.to_string());
                this.dataSent.markAcked(ack);
            }
        }

        if (CUMULATIVE_ACK && anyDataRcvd) {
            // we can only cumulatively ACK the highest segment for which we have received all preceding segments; it
            // may well not be the latest data segment we just read because we may still have "holes" in our message
            // from preceding segments we haven't received due to missing, delayed, or corrupt segments that have yet
            // to be retransmitted.
            //
            // Our contract is that we're ACKing back to the client "we're rcvd all your segments up to 'highest', so
            // you can consider all of them ACK'ed, even though we didn't individually ACK each segment.
            //
            // Now, it's possible we've already done this before, because we're still waiting on retransmits.  And we
            // can't keep track of what we've already sent ACKs on, because with the unreliable channel, we don't
            // know that our ACKs were acktually (pun) received.  We don't ACK the ACKs.  But the client's ok with
            // receiving re-ACKs.  They are a no-nop.
            sendAck(this.dataRcvd.getHighestContiguousSequence());
        }

        if (DEBUG) System.out.println(" processReceiveAndSendRespond(): data received so far: '"
                                      + this.getDataReceived()
                                      + "'");
    }



    /**
     * Dump to the console all the current data (message) segments.  This is for diagnostic purposes.
     * <p>
     * Called by main.
     */
    public void dumpAllSegments() {

        // note: we want to do this on request, regardless of the state of DEBUG
        boolean currentDEBUG = DEBUG;
        try {
            DEBUG = true;
            this.dataSent.dump();
            System.out.println("All data ACKed: " + this.dataSent.getIsEverythingAcked());
            System.out.println();
        } finally {
            // guarantee restoration even if an exception was thrown
            DEBUG = currentDEBUG;
        }
    }



    private void processTimeouts() {

        // find all the packets that have been waiting too long for an ACK, and resend them to the server
        int timeoutThreshold = this.currentIteration - TIME_OUT_ITERATIONS;
        this.dataSent.getAllPackets()
                     .stream()
                     .filter(p -> !p.getIsAcked())
                     .filter(p -> p.getStartIteration() < timeoutThreshold)
                     .forEach(p -> resendData(p));
    }



    private void sendAck(int sequenceNumber) {

        if (sequenceNumber == -1) {
            // this means that we don't have contiguous segments ACKed yet.  There's nothing for us to do yet.
            return;
        }
        Segment segmentAck = new Segment();
        segmentAck.setAck(sequenceNumber);
        if (DEBUG) System.out.println("Sending ACK: " + segmentAck.to_string());

        // Use the unreliable sendChannel to send the ack packet
        this.sendChannel.send(segmentAck);
    }



    private void sendData(int sequenceNumber, String data) {

        // build the segment
        Segment segmentToSend = new Segment();
        segmentToSend.setData(sequenceNumber, data);
        segmentToSend.setStartIteration(this.currentIteration);

        // Use the unreliable sendChannel to send the segment
        if (DEBUG) System.out.println("sendData():     sending segment: " + segmentToSend.to_string());
        this.sendChannel.send(segmentToSend);

        // start tracking this segment for ACKing
        this.dataSent.addPacket(segmentToSend);

        // reduce the flow control window's capacity
        this.currentWindowCapacity = Math.max(0, this.currentWindowCapacity - data.length());
    }



    private void resendData(Segment segment) {

        // use the segment's original sequence number, so the server knows where it goes in the message.  However, it
        // will be sent with the current iteration, which effectively restarts the timeout clock; it may take
        // multiple resends across the unreliable channel to get this segment successfully to the server!
        if (DEBUG) System.out.println("resendSegment():    re-sending segment: " + segment.to_string());

        // re-sends need to obey flow control, too.  If we don't have enough flow control window capacity, then we
        // make a possibly risky assumption that the server really has capacity because just the fact that we're
        // doing a resend probably means that a segment was dropped, or had a bad checksum, or is delayed.  We can't
        // determine the reason from the client side, so we'll take the risk of the resend.  We need to game the
        // window capacity numbers so that we don't allow new data to go out until we're sure there's room.
        int resendLen = segment.getPayload().length();
        if (this.currentWindowCapacity < resendLen) {
            this.currentWindowCapacity = resendLen;
        }

        // resend the segment, but in it's original sequence
        sendData(segment.getSegmentNumber(), segment.getPayload());
        ++this.countSegmentTimeouts;
    }



    private int getCurrentSequenceNumber() {

        return this.currentSegmentNumber++;
    }



    private boolean isData(Segment segment) {

        return !isAck(segment);
    }



    private boolean isAck(Segment segment) {

        // only ACKs have a positive ACK number; data segments have -1
        return segment.getAckNumber() >= 0;
    }



    /**
     * Status tracker for our data segments so we can detect un-ACKed segments due to data corruption, dropouts, delays,
     * etc.
     */
    private class Message {

        // our segments in sequence order
        private final Map<Integer, Packet> packets = new TreeMap<>();



        public void dump() {

            if (!DEBUG) return;

            System.out.println("\nMessage");
            System.out.println("-------");
            packets.entrySet()
                   .forEach(p -> System.out.println("    {"
                                                    + p.getKey().toString()
                                                    + ", "
                                                    + p.getValue().toString()
                                                    + " }"));
            System.out.println();
        }



        public boolean getIsEverythingAcked() {

            // look for the first packet that isn't ACKed
            return !packets.values().stream().anyMatch(p -> !p.getIsAcked());
        }



        public void markAcked(Segment ack) {

            boolean anythingAcked = false;

            if (!isAck(ack)) {
                System.out.println("YIKES!!!!! trying to ACK with a non-ACK segment! " + ack.to_string());
                return;
            }

            if (CUMULATIVE_ACK) {
                // first check to see if the server is just re-acking what it's already cumulatively ACK'ed because
                // it's waiting on resends for missing segments.
                if (ack.getAckNumber() == lastCumulativeAck) {
                    // no sense redoing acks on what's already been acked.
                    return;
                }

                // cumulative ACK: mark this segment and all its predecessors ACKed
                for (int seqNum = 0; seqNum <= ack.getAckNumber(); seqNum++) {
                    Packet dataPacket = getPacket(seqNum);
                    if (dataPacket == null) {
                        System.out.println("YIKES!!!!! trying to cumulative ACK packet #"
                                           + ack.getAckNumber()
                                           + " but it doesn't exist!!!");
                        // do not attempt to continue! we have a hole in the contiguous segments; this is a bug!
                        return;
                    }
                    if (!dataPacket.getIsAcked()) {
                        dataPacket.setIsAcked();
                        anythingAcked = true;

                        // recover the flow control capacity of the ACKed segment
                        if (DO_FLOW_CONTROL) {
                            int recoveredLen = dataSent.getPacket(ack.getAckNumber()).getPayload().length();
                            currentWindowCapacity = Math.min(FLOW_CONTROL_WIN_SIZE,
                                                             currentWindowCapacity + recoveredLen);
                        }
                    }
                }
            }

            else {
                // we're ACKing every segment
                Packet dataPacket = getPacket(ack.getAckNumber());
                if (dataPacket == null) {
                    System.out.println("YIKES!!!!! trying to ack packet #"
                                       + ack.getAckNumber()
                                       + " but it doesn't exist!!!");
                    return;
                }
                if (dataPacket.getIsAcked()) {
                    // this can happen with delayed packets.  The client doesn't get an ACK, so it resends.  But then
                    // the delayed packet gets to the server and it ACKs.  Then we get here when the server re-ACKs
                    // our resend.
                    return;
                }
                dataPacket.setIsAcked();
                anythingAcked = true;

                if (DO_FLOW_CONTROL) {
                    // recover the window space used by this ACK'ed segment
                    int recoveredLen = dataSent.getPacket(ack.getAckNumber()).getPayload().length();
                    currentWindowCapacity = Math.min(FLOW_CONTROL_WIN_SIZE,
                                                     currentWindowCapacity + recoveredLen);
                }
            }

            if (anythingAcked) dump();
        }



        public int getHighestContiguousSequence() {

            int highest = -1;
            while (packets.get(++highest) != null) ;

            return --highest;
        }



        public Packet getPacket(int sequenceNumber) {

            return packets.get(sequenceNumber);
        }



        public void addPacket(Segment segment) {

            addPacket(new Packet(segment));
        }



        public void addPacket(Packet packet) {

            packets.put(packet.getSequenceNumber(), packet);
        }



        public List<Packet> getAllPackets() {

            // copy-safe
            return new ArrayList<>(packets.values());
        }



        public String getAllData() {

            StringBuilder sb = new StringBuilder();
            getAllPackets().stream().forEach(p -> sb.append(p.getPayload()));
            return sb.toString();
        }
    }



    private class Packet extends Segment {

        private boolean isAcked = false;



        public Packet(Segment segment) {

            super();
            super.setData(segment.getSegmentNumber(), segment.getPayload());
            super.setStartIteration(segment.getStartIteration());
        }



        public String toString() {

            return "[iter=" + getStartIteration() + ", ack=" + getIsAcked() + ", " + to_string() + "]";
        }



        public void setIsAcked() {

            isAcked = true;
        }



        public boolean getIsAcked() {

            return isAcked;
        }



        public int getSequenceNumber() {

            return getSegmentNumber();
        }
    }
}
