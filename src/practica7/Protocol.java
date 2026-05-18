package practica7;

import util.Protocol_base;
import util.TCPSegment;
import util.SimNet;
import util.TSocket_base;

public class Protocol extends Protocol_base {

    protected Protocol(SimNet network) {
        super(network);
    }

    public void ipInput(TCPSegment segment) {

        TSocket_base sc = getMatchingTSocket(segment.getDestinationPort(), segment.getSourcePort());
        if (sc != null) {

            sc.processReceivedSegment(segment);
        } else {

            log.printRED("ipInput: no s'ha trobat socket per al segment: " + segment);
        }
    }

    protected TSocket_base getMatchingTSocket(int localPort, int remotePort) {
        lk.lock();
        try {
            for (TSocket_base sc : this.activeSockets) {

                if (sc.localPort == localPort && sc.remotePort == remotePort) {
                    return sc;
                }
            }

            for (TSocket_base sc : this.listenSockets) {

                if (sc.localPort == localPort) {

                    return sc;
                }
            }

            return null;
        } finally {
            lk.unlock();
        }
    }

}
