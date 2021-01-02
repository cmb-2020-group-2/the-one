/*
 * Copyright 2010 Aalto University, ComNet
 * Released under GPLv3. See LICENSE.txt for details.
 */
package report;

import core.DTNHost;
import core.Message;
import core.MessageListener;

/**
 * Reports delivered messages
 * report csv:
 *  message_id, from, to, creation_time, host_location
 */
public class CovidTransmissionReport extends Report implements MessageListener {
    public static final String HEADER =
            "message_id, from, to, creation_time";
    /** all message delays */

    /**
     * Constructor.
     */
    public CovidTransmissionReport() {
        init();
    }

    @Override
    public void init() {
        super.init();
        write(HEADER);
    }

    public void newMessage(Message m) {}

    public void messageTransferred(Message m, DTNHost from, DTNHost to,
                                   boolean finalTarget) {
        String event_string = m.getId() + " "
                + from.toString() + " "
                + to.toString() + " "
                + format(getSimTime()) + " "
                + from.getLocation();
        System.out.println(event_string);
        write(event_string);

    }

    @Override
    public void done() {
        super.done();
    }

    // nothing to implement for the rest
    public void messageDeleted(Message m, DTNHost where, boolean dropped) {}
    public void messageTransferAborted(Message m, DTNHost from, DTNHost to) {}
    public void messageTransferStarted(Message m, DTNHost from, DTNHost to) {}

}
