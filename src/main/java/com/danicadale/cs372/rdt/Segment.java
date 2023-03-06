package com.danicadale.cs372.rdt;
/* ****************************************************************************************************************** */
/* Segment                                                                                                            */
/*                                                                                                                    */
/* Description:                                                                                                       */
/* The segment is a segment of data to be transferred on a communication channel.                                     */
/*                                                                                                                    */
/* Notes:                                                                                                             */
/* This file is not to be changed.                                                                                    */
/*                                                                                                                    */
/*                                                                                                                    */
/*                                                                                                                    */
/*                                                                                                                    */
/*                                                                                                                    */
/*                                                                                                                    */
/*                                                                                                                    */
/*                                                                                                                    */
/* ****************************************************************************************************************** */

import java.util.Random;

public class Segment {
    private int seqmentNumber = -1;
    private int ackNumber = -1;
    private String payload = "";
    private int checksum = 0;
    private int startIteration = 0;
    private int startDelayIteration = 0;

    /* ************************************************************************************************************** */
    /* Getters                                                                                                        */
    /*                                                                                                                */
    /*                                                                                                                */
    /*                                                                                                                */
    /*                                                                                                                */
    /* ************************************************************************************************************** */

    public int getAckNumber()
    {
        return this.ackNumber;
    }

    public int getStartIteration()
    {
        return this.startIteration;
    }

    public int getStartDelayIteration()
    {
        return this.startDelayIteration;
    }

    // ### There was no getter for the data or the segmentNumber in the original code
    public String getPayload() { return this.payload; }
    public int getSegmentNumber() { return this.seqmentNumber; }

    /* ************************************************************************************************************** */
    /* Setters                                                                                                        */
    /*                                                                                                                */
    /*                                                                                                                */
    /*                                                                                                                */
    /*                                                                                                                */
    /* ************************************************************************************************************** */
    public void setData(int seq, String data)
    {
        this.seqmentNumber = seq;
        this.ackNumber = -1;
        this.payload = data;
        this.checksum = 0;
        this.checksum = this.calc_checksum(this.to_string());
    }

    public void setAck(int ack)
    {
        this.seqmentNumber = -1;
        this.ackNumber = ack;
        this.payload = "";
        this.checksum = 0;
        this.checksum = this.calc_checksum(this.to_string());
    }

    public void setStartIteration(int iteration)
    {
        this.startIteration = iteration;
    }

    public void setStartDelayIteration(int iteration)
    {
        this.startDelayIteration = iteration;
    }

    /* ************************************************************************************************************** */
    /* Public Functions                                                                                               */
    /*                                                                                                                */
    /*                                                                                                                */
    /*                                                                                                                */
    /*                                                                                                                */
    /* ************************************************************************************************************** */

    public String to_string()
    {
        return ("seq: " + this.seqmentNumber + ", ack: " + this.ackNumber + ", data: " + this.payload);
    }

    public boolean checkChecksum()
    {
        int cs = this.calc_checksum(this.to_string());
        return cs == this.checksum;
    }

    public int calc_checksum(String str)
    {
        int cs = 0;
        for ( int i = 0; i < str.length(); i++ )
        {
            cs += str.charAt(i);
        }
        return cs;
    }

    public void printToConsole()
    {
        System.out.print(this.to_string());
    }

    // Function to cause an error - Do not modify
    public void createChecksumError()
    {
        if (this.payload.isEmpty())
        {
            return;
        }

        Random random = new Random();

        // ### bug: original did not check for negative random number, random.nextInt() will create.  This caused
        // substring() to blow up
        int char_pos = Math.abs(random.nextInt() % this.payload.length());

        // Replace character
        this.payload = this.payload.substring(0, char_pos) + 'X' + this.payload.substring(char_pos + 1);
    }
}
