package com.danicadale.cs372.rdt;

/* ****************************************************************************************************************** */
/* RDTLayer                                                                                                           */
/*                                                                                                                    */
/* Description:                                                                                                       */
/* The reliable data transfer (RDT) layer is used as a communication layer to resolve issues over an unreliable       */
/* channel.                                                                                                           */
/*                                                                                                                    */
/*                                                                                                                    */
/* Notes:                                                                                                             */
/* This file is meant to be changed.                                                                                  */
/*                                                                                                                    */
/*                                                                                                                    */
/*                                                                                                                    */
/*                                                                                                                    */
/* ****************************************************************************************************************** */

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;



/**
 * The reliable data transfer (RDT) layer is used as a communication layer to resolve issues over an unreliable channel
 *
 * @author Danica Dale   CS-372
 * @since March 2023
 */
public class RDTLayer {

    /* ************************************************************************************************************** */
    /* Class Scope Variables                                                                                          */
    /*                                                                                                                */
    /*                                                                                                                */
    /*                                                                                                                */
    /*                                                                                                                */
    /*                                                                                                                */
    /* ************************************************************************************************************** */


    // Max length of the string data that will be sent per packet (in chars, not bytes)
    public static final int DATA_LENGTH = 4;

    // Max amount of data to send in a flow control window (in chars, not bytes)
    public static final int FLOW_CONTROL_WIN_SIZE = 15;

    private static final int lastCumulativeAck = -1;

    // any sent data not ACKed in this many iterations is a timeout->resend
    private final int TIME_OUT_ITERATIONS = 3;

    // set this true to implement cumulative ACKing
    private final boolean CUMULATIVE_ACK = true;

    // ###
    private final String whom;

    private final Message dataSent = new Message();  // used by client

    private final Message dataRcvd = new Message();  // used by server


    // effectively, the number of time we have to resend a segment because we didn't get an ACK for it
    private int countSegmentTimeouts = 0;

    private UnreliableChannel sendChannel = null;

    private UnreliableChannel receiveChannel = null;

    private String dataToSend = "";

    // used as our network clock
    private int currentIteration = 0;

    // sequence counter
    private int currentSegmentNumber = 0;



    /* ************************************************************************************************************** */
    /* Constructors                                                                                                   */
    /*                                                                                                                */
    /*                                                                                                                */
    /*                                                                                                                */
    /*                                                                                                                */
    /*                                                                                                                */
    /* ************************************************************************************************************** */
    public RDTLayer(String whom) {
        // Add items as needed
        this.whom = whom + ": ";
    }



    /* ************************************************************************************************************** */
    /* setSendChannel()                                                                                               */
    /*                                                                                                                */
    /* Description:                                                                                                   */
    /* Called by main to set the unreliable sending lower-layer channel                                               */
    /*                                                                                                                */
    /*                                                                                                                */
    /* ************************************************************************************************************** */
    public void setSendChannel(UnreliableChannel channel) {

        this.sendChannel = channel;
    }



    /* ************************************************************************************************************** */
    /* setReceiveChannel()                                                                                            */
    /*                                                                                                                */
    /* Description:                                                                                                   */
    /* Called by main to set the unreliable receiving lower-layer channel                                             */
    /*                                                                                                                */
    /*                                                                                                                */
    /* ************************************************************************************************************** */
    public void setReceiveChannel(UnreliableChannel channel) {

        this.receiveChannel = channel;
    }



    /**
     * Fetch the data this RDT Layer is sending.
     *
     * @return the data this RDT Layer is sending.  If no data has been fed to this layer, or, we are a server, then
     *         null is returned.
     *
     * @see #setDataToSend(String)
     */
    public String getDataToSend() {

        return dataToSend;
    }



    /* ************************************************************************************************************** */
    /* setDataToSend()                                                                                                */
    /*                                                                                                                */
    /* Description:                                                                                                   */
    /* Called by main to set the string data to send                                                                  */
    /*                                                                                                                */
    /*                                                                                                                */
    /* ************************************************************************************************************** */
    public void setDataToSend(String data) {

        this.dataToSend = data;
    }



    /* ************************************************************************************************************** */
    /* getDataReceived()                                                                                              */
    /*                                                                                                                */
    /* Description:                                                                                                   */
    /* Called by main to get the currently received and buffered string data, in order                                */
    /*                                                                                                                */
    /*                                                                                                                */
    /* ************************************************************************************************************** */
    public String getDataReceived() {

        System.out.println(whom + "getDataReceived():    all: '" + dataRcvd.getAllData() + "'");
        return dataRcvd.getAllData();
    }



    /* ************************************************************************************************************** */
    /* getCountSegmentTimeouts()                                                                                      */
    /*                                                                                                                */
    /* Description:                                                                                                   */
    /* Called by main to get the count of segment timeouts                                                            */
    /*                                                                                                                */
    /*                                                                                                                */
    /* ************************************************************************************************************** */
    public int getCountSegmentTimeouts() {

        return this.countSegmentTimeouts;
    }



    /* ************************************************************************************************************** */
    /* processData()                                                                                                  */
    /*                                                                                                                */
    /* Description:                                                                                                   */
    /* "timeslice". Called by main once per iteration                                                                 */
    /*                                                                                                                */
    /*                                                                                                                */
    /* ************************************************************************************************************** */
    public void processData() {

        this.currentIteration++;

        // first, deal with anything that's been received.
        processReceiveAndSendRespond();

        // now that we've dealt with any ACKs that have come in...
        processTimeouts();

        // then, do any sending
        processSend();
    }



    /* ************************************************************************************************************** */
    /* processSend()                                                                                                  */
    /*                                                                                                                */
    /* Description:                                                                                                   */
    /* Manages Segment sending tasks                                                                                  */
    /*                                                                                                                */
    /*                                                                                                                */
    /* ************************************************************************************************************** */
    public void processSend() {

        /* ********************************************************************************************************** */
        System.out.println(whom + "processSend(): Complete this...");

        /* You should pipeline segments to fit the flow-control window
         * The flow-control window is the constant RDTLayer.FLOW_CONTROL_WIN_SIZE
         * The maximum data that you can send in a segment is RDTLayer.DATA_LENGTH
         * These constants are given in # characters

         * Somewhere in here you will be creating data segments to send.
         * The data is just part of the entire string that you are trying to send.
         * The seqnum is the sequence number for the segment (in character number, not bytes)
         */

        System.out.println(whom + "processSend(): BEGIN");


        // if we don't have anything to send, then:
        // -- We are either the server (which only sends ACKs, not data)
        // OR
        // -- We're the client and we've sent all of the entire message (but we may well still be running waint on
        //    ACKs and dealing with resends).
        while (!this.dataToSend.isEmpty()) {

            //
            // NOTE: until windowing is in place, this will send the ENTIRE msg!  It will bombard the server!
            //

            // carve out a segment from the leading portion of the remaining data we have to send
            int len = Math.min(this.dataToSend.length(), DATA_LENGTH);
            System.out.println(whom
                               + "processSend():     pre this.dataToSend: '"
                               + this.dataToSend
                               + "' ("
                               + this.dataToSend.length()
                               + ")");
            System.out.println(whom + "processSend():     len to send: " + len);
            String data = "";
            if (len > 0) {
                data = this.dataToSend.substring(0, len);
                if (len == this.dataToSend.length()) {
                    // nothing more to send
                    this.dataToSend = "";
                }
                else {
                    this.dataToSend = this.dataToSend.substring(len);
                }
            }
            else {
                data = "";
            }
            System.out.println(whom
                               + "processSend():     post this.dataToSend: '"
                               + this.dataToSend
                               + "' ("
                               + this.dataToSend.length()
                               + ")");
            System.out.println(whom + "processSend():     data: '" + data + "' (" + data.length() + ")");

            /*
             * ********************************************************************************************************** */
            // Display sending segment
            sendSegment(getCurrentSequenceNumber(), data);

            if (false) {
                Segment segmentToSend = new Segment();
                segmentToSend.setData(getCurrentSequenceNumber(), data);
                segmentToSend.setStartIteration(this.currentIteration);

                // Use the unreliable sendChannel to send the segment
                System.out.println(whom + "processSend():     sending segment: " + segmentToSend.to_string());
                this.sendChannel.send(segmentToSend);
                this.dataSent.addPacket(segmentToSend);
                System.out.println(whom + "processSend(): END");
            }
        }
    }



    /* ************************************************************************************************************** */
    /* processReceive()                                                                                               */
    /*                                                                                                                */
    /* Description:                                                                                                   */
    /* Manages Segment receive tasks                                                                                  */
    /*                                                                                                                */
    /*                                                                                                                */
    /* ************************************************************************************************************** */
    public void processReceiveAndSendRespond() {

        System.out.println(whom + "processReceiveAndSendRespond() BEGIN");

        // This call returns a list of incoming segments (see Segment class)...
        ArrayList<Segment> listIncomingSegments = this.receiveChannel.receive();

        /* ********************************************************************************************************** */
        // What segments have been received?
        // How will you get them back in order?
        // This is where a majority of your logic will be implemented
        System.out.println(whom
                           + "processReceiveAndSendRespond()    received: "
                           + listIncomingSegments.size()
                           + " segments");
        if (listIncomingSegments.isEmpty()) {
            System.out.println(whom + "processReceiveAndSendRespond()    nothing received; bailing out");
            return;
        }
        for (Segment segment : listIncomingSegments) {
            System.out.println(whom + "processReceiveAndSendRespond()    received:     " + segment.to_string());
        }

        /* ********************************************************************************************************** */
        // How do you respond to what you have received?
        // How can you tell data segments apart from ack segemnts?
        System.out.println(whom + "### processReceiveAndSendRespond():    remembering what we've received...");
        boolean anyDataRcvd = false;
        for (Segment segmentRcvd : listIncomingSegments) {

            int highestSequenceRcvd = -1;

            if (isData(segmentRcvd)) {
                // we should only be here if we're a server receiving from a client
                System.out.println(whom
                                   + "### processReceiveAndSendRespond():    remembering "
                                   + segmentRcvd.to_string());

                if (!segmentRcvd.checkChecksum()) {
                    System.out.println(whom
                                       + "### processReceiveAndSendRespond():    bad checksum!!! on "
                                       + segmentRcvd.to_string());
                    // bail out; don't ACK.  The client will retransmit when it times out on not getting the ACK
                    continue;
                }
                dataRcvd.addPacket(segmentRcvd);
                anyDataRcvd = true;

                // ack the rcvd segment
                if (!CUMULATIVE_ACK) {
                    // we're ACKing every segment received from the client; very chatty
                    sendAck(segmentRcvd.getSegmentNumber());
                }
                else {
                    // keep track for sending a cumulative ack after we've received all the incoming segments
                    highestSequenceRcvd = Math.max(highestSequenceRcvd, segmentRcvd.getSegmentNumber());
                }
            }
            else if (isAck(segmentRcvd)) {
                // we should only be here if we're a client (servers don't rcv ACKs)
                Segment ack = segmentRcvd;
                System.out.println(whom
                                   + "### processReceiveAndSendRespond():    processing ACK for "
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

        System.out.println(whom
                           + "### processReceiveAndSendRespond():    this.getDataReceived(): '"
                           + this.getDataReceived()
                           + "'");

        System.out.println(whom + "### processReceiveAndSendRespond() END\n\n");
    }



    public boolean getIsAllDataAcked() {

        return dataSent.getIsEverythingAcked();
    }



    public void dumpAllSegments() {

        dataSent.dump();
        System.out.println("All data ACKed: " + getIsAllDataAcked());
        System.out.println();
    }



    private void processTimeouts() {

        // for timeouts
        int timeoutThreshold = this.currentIteration - this.TIME_OUT_ITERATIONS;
        for (Packet packet : dataSent.getAllPackets()) {
            if (!packet.getIsAcked()) {
                if (packet.getStartIteration() < timeoutThreshold) {
                    resendSegment(packet);
                }
            }
        }
    }



    private void sendAck(int sequenceNumber) {

        if (sequenceNumber == -1) {
            // this means that we don't have contiguous segments ACKed yet.  There's nothing for us to do yet.
            return;
        }
        Segment segmentAck = new Segment();     // Segment acknowledging packet(s) received
        segmentAck.setAck(sequenceNumber);
        System.out.println(whom
                           + "Sending ACK: "
                           + segmentAck.to_string());
        // Use the unreliable sendChannel to send the ack packet
        this.sendChannel.send(segmentAck);
    }



    private void sendSegment(int sequenceNumber, String data) {

        Segment segmentToSend = new Segment();
        segmentToSend.setData(sequenceNumber, data);
        segmentToSend.setStartIteration(this.currentIteration);

        // Use the unreliable sendChannel to send the segment
        System.out.println(whom + "sendSegment():     sending segment: " + segmentToSend.to_string());
        this.sendChannel.send(segmentToSend);
        this.dataSent.addPacket(segmentToSend);
    }



    private void resendSegment(Segment segment) {

        // use the segment's original sequence number, so the server knows where it goes in the message.  However, it
        // will be sent with the current iteration, which effectively restarts the timeout clock; it may take
        // multiple resends across the unreliable channel to get this segment successfully to the server!
        System.out.println(whom + "resendSegment():    re-sending segment: " + segment.to_string());
        sendSegment(segment.getSegmentNumber(), segment.getPayload());
        ++countSegmentTimeouts;
    }



    private int getCurrentSequenceNumber() {

        return this.currentSegmentNumber++;
    }



    private boolean isData(Segment segment) {

        return !isAck(segment);
    }



    private boolean isAck(Segment segment) {

        return segment.getAckNumber() >= 0;
    }



    private class Message {


        private final Map<Integer, Packet> packets = new TreeMap<>();

        private String wholeMessage;



        public void dump() {

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

                // mark this segment and all its predecessors ACKed
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
                        // we don't really care if it's already been ACKed.. we could've re-ACKed it anyway
                        System.out.println("\nCUMULATIVE ACK: " + dataPacket.getSequenceNumber() + "\n");
                        dataPacket.setIsAcked();
                        anythingAcked = true;
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
                    // the
                    // delayed packet gets to the server and it ACKs.  Then we get here when the server re-ACKs our
                    // resend.
                    System.out.println("IHHHHHHH?  acking packet #" + ack.getAckNumber() + " but it is already acked!");
                    return;
                }
                dataPacket.setIsAcked();
                anythingAcked = true;
                System.out.println("Packet #" + ack.getAckNumber() + " marked ACKed!");
            }

            if (anythingAcked) dump();
        }



        public int getHighestContiguousSequence() {

            int highest = -1;
            while (packets.get(++highest) != null) ;

            return --highest;
        }



        public void setWholeMessage(String wholeMessage) {

            this.wholeMessage = wholeMessage;
        }



        public Packet getPacket(int sequenceNumber) {

            return packets.get(sequenceNumber);
        }



        public void addPacket(Segment segment) {

            System.out.println("Message.addPacket(" + segment.to_string() + ")");
            addPacket(new Packet(segment));
        }



        public void addPacket(Packet packet) {

            packets.put(packet.getSequenceNumber(), packet);
        }



        public List<Packet> getAllPackets() {

            // copy-safe
            List<Packet> all = new ArrayList<>(packets.size());
            all.addAll(packets.values());
            return all;
        }



        public String getAllData() {

            StringBuilder sb = new StringBuilder();
            for (Packet packet : getAllPackets()) {
                sb.append(packet.getPayload());
            }
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
