package com.danicadale.cs372.rdt;



import java.io.IOException;

public class RDT_Main {
    public static void main(String[] args)
    {
        /* ********************************************************************************************************** */
        /* The following are two sets of data input to the communication test. The first is small and the second is   */
        /* longer. Start by uncomming the shorter until you feel you have a good algorithm. Then confirm it still     */
        /* works on a larger scale by switching to the larger.                                                        */
        /* ********************************************************************************************************** */
        String shortSentence = "The quick brown fox jumped over the lazy dog";

        String longParagraph = "\r\n\r\n...We choose to go to the moon. We choose to go to the moon in this " +
        "decade and do the other things, not because they are easy, but because they are hard, " +
        "because that goal will serve to organize and measure the best of our energies and skills, " +
        "because that challenge is one that we are willing to accept, one we are unwilling to " +
        "postpone, and one which we intend to win, and the others, too." +
        "\r\n\r\n" +
        "...we shall send to the moon, 240,000 miles away from the control station in Houston, a giant " +
        "rocket more than 300 feet tall, the length of this football field, made of new metal alloys, " +
        "some of which have not yet been invented, capable of standing heat and stresses several times " +
        "more than have ever been experienced, fitted together with a precision better than the finest " +
        "watch, carrying all the equipment needed for propulsion, guidance, control, communications, food " +
        "and survival, on an untried mission, to an unknown celestial body, and then return it safely to " +
        "earth, re-entering the atmosphere at speeds of over 25,000 miles per hour, causing heat about half " +
        "that of the temperature of the sun--almost as hot as it is here today--and do all this, and do it " +
        "right, and do it first before this decade is out.\r\n\r\n" +
        "JFK - September 12, 1962\r\n";


        //String dataToSend = shortSentence;
        String dataToSend = longParagraph;

        /* ********************************************************************************************************** */

        // Create client and server
        // ### remove the ID of whom the RDTLayer belongs to
        RDTLayer rdt_client = new RDTLayer("CLIENT");
        RDTLayer rdt_server = new RDTLayer("SERVER");

        // Start with a reliable channel (all flags false)
        // As you create your rdt algorithm for send and receive, turn these on.
        boolean outOfOrder = false;
        boolean dropPackets = false;
        boolean delayPackets = false;
        boolean dataErrors = false;

        // Create unreliable communication channels
        UnreliableChannel clientToServerChannel = new UnreliableChannel("clientToServerChannel", outOfOrder, dropPackets, delayPackets, dataErrors);
        UnreliableChannel serverToClientChannel = new UnreliableChannel("serverToClientChannel", outOfOrder, dropPackets, delayPackets, dataErrors);

        // Creat client and server that connect to unreliable channels
        rdt_client.setSendChannel(clientToServerChannel);
        rdt_client.setReceiveChannel(serverToClientChannel);
        rdt_server.setSendChannel(serverToClientChannel);
        rdt_server.setReceiveChannel(clientToServerChannel);

        // Set initial data that will be sent from client to server
        rdt_client.setDataToSend(dataToSend);

        int loopIter = 0;                           // Used to track communication timing in iterations
        while(true) {
            System.out.println("-----------------------------------------------------------------------------------------------------------");
            loopIter++;
            System.out.println("Time (iterations) = " + loopIter);

            // Sequence to pass segments back and forth between client and server
            System.out.println("Client------------------------------------------");
            rdt_client.processData();
            clientToServerChannel.processData();
            System.out.println("Server------------------------------------------");
            rdt_server.processData();
            serverToClientChannel.processData();


            // show the data received so far
            System.out.println("Main--------------------------------------------");
            String dataReceivedFromClient = rdt_server.getDataReceived();
            System.out.println("DataReceivedFromClient: '" + dataReceivedFromClient + "'");

            if (dataReceivedFromClient.equals(dataToSend))
            {
                System.out.println("$$$$$$$$ ALL DATA RECEIVED $$$$$$$$");
                break;
            }

            // Used to slow down display for each round when you don't want it to be as interactive
            // with pressing enter to go to the next round.
            // Adjust from 1 to 1000 for 1 second delay. Value is in milliseconds.
            try
            {
                Thread.sleep(1);
            }
            catch (InterruptedException e)
            {
                e.printStackTrace();
            }

            // Wait for user to press enter for next iteration
            try
            {
                System.out.print("\nHit Enter to continue: ");
                if (false) System.in.read();
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
        }

        System.out.println("countTotalDataPackets: " + clientToServerChannel.getCountTotalDataPackets());
        System.out.println("countSentPackets: " + (clientToServerChannel.getCountSentPackets() + serverToClientChannel.getCountSentPackets()));
        System.out.println("countChecksumErrorPackets: " + clientToServerChannel.getCountChecksumErrorPackets());
        System.out.println("countOutOfOrderPackets: " + clientToServerChannel.getCountOutOfOrderPackets());
        System.out.println("countDelayedPackets: " + (clientToServerChannel.getCountDelayedPackets() + serverToClientChannel.getCountDelayedPackets()));
        System.out.println("countDroppedDataPackets: " + clientToServerChannel.getCountDroppedPackets());
        System.out.println("countAckPackets: " + serverToClientChannel.getCountAckPackets());
        System.out.println("countDroppedAckPackets: " + serverToClientChannel.getCountDroppedPackets());

        System.out.println("# segment timeouts: " + rdt_client.getCountSegmentTimeouts());

        System.out.println("TOTAL ITERATIONS: " + loopIter);

    }
}
