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
import java.util.Map.Entry;
import java.util.TreeMap;



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

    private final int countSegmentTimeouts = 0;

    // ###
    private final String whom;
    private final Message dataSent = new com.danicadale.cs372.rdt.RDTLayer.Message();
    private final Message dataRcvd = new com.danicadale.cs372.rdt.RDTLayer.Message();
    private UnreliableChannel sendChannel = null;
    private UnreliableChannel receiveChannel = null;
    private String dataToSend = "";
    // Use this for segment 'timeouts'
    private int currentIteration = 0;

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

        // carve off the leading window of the data to send
        System.out.println(whom + "processSend(): BEGIN");
        if (this.dataToSend.isEmpty()) {
            System.out.println(whom
                               + "processSend():    we have nothing to send (we're probably the server); bailing out");
            return;
        }
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
            if (len < this.dataToSend.length()) {
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

        /* ********************************************************************************************************** */
        // Display sending segment
        Segment segmentToSend = new Segment();
        segmentToSend.setData(getCurrentSegmentNumber(), data);
        segmentToSend.setStartIteration(this.currentIteration);

        // Use the unreliable sendChannel to send the segment
        System.out.println(whom + "processSend():     sending segment: " + segmentToSend.to_string());
        this.sendChannel.send(segmentToSend);
        this.dataSent.addPacket(segmentToSend);
        System.out.println(whom + "processSend(): END");
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
        for (Segment segmentRcvd : listIncomingSegments) {
            if (isData(segmentRcvd)) {
                System.out.println(whom
                                   + "### processReceiveAndSendRespond():    remembering "
                                   + segmentRcvd.to_string());
                dataRcvd.addPacket(segmentRcvd);

                // ack the rcvd segment
                Segment segmentAck = new Segment();     // Segment acknowledging packet(s) received
                segmentAck.setAck(segmentRcvd.getSegmentNumber());
                System.out.println(whom
                                   + "### processReceiveAndSendRespond():    Sending ACK: "
                                   + segmentAck.to_string());
                // Use the unreliable sendChannel to send the ack packet
                this.sendChannel.send(segmentAck);
            }
            else if (isAck(segmentRcvd)) {
                Segment ack = segmentRcvd;
                System.out.println(whom
                                   + "### processReceiveAndSendRespond():    processing ACK for "
                                   + ack.to_string());
                this.dataSent.markAcked(ack);
            }
        }
        System.out.println(whom
                           + "### processReceiveAndSendRespond():    this.getDataReceived(): '"
                           + this.getDataReceived()
                           + "'");

        System.out.println(whom + "### processReceiveAndSendRespond() END\n\n");
    }



    private int getCurrentSegmentNumber() {

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
            for (Entry<Integer, Packet> packet : packets.entrySet()) {
                System.out.println("    {" + packet.getKey().toString() + ", " + packet.getValue().toString() + " }");
            }
            System.out.println();
        }



        public void markAcked(Segment ack) {

            Packet dataPacket = getPacket(ack.getAckNumber());
            if (dataPacket == null) {
                System.out.println("YIKES!!!!! trying to ack packet #"
                                   + ack.getAckNumber()
                                   + " but it doesn't exist!!!");
                return;
            }
            if (dataPacket.getIsAcked()) {
                System.out.println("IHHHHHHH?  acking packet #" + ack.getAckNumber() + " but it is already acked!");
                return;
            }
            dataPacket.setIsAcked();
            System.out.println("Packet #" + ack.getAckNumber() + " marked ACKed!");
            dump();
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
            dump();
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
