package practica4;

import util.Protocol_base;
import util.TCPSegment;
import util.SimNet;
import util.TSocket_base;

public class Protocol extends Protocol_base {

    public Protocol(SimNet network) {
        super(network);
    }

    protected void ipInput(TCPSegment seg) {

        TSocket_base sc = getMatchingTSocket(seg.getDestinationPort(), seg.getSourcePort());
        if(sc != null){
        
            sc.processReceivedSegment(seg);
        } else{
        
            log.printRED("ipInput: no s'ha trobat socket per al segment: " + seg);
        }
        
    }

    protected TSocket_base getMatchingTSocket(int localPort, int remotePort) {
        lk.lock();
        try {

            for (TSocket_base sc : this.activeSockets) {

                if ( sc.localPort == localPort && sc.remotePort == remotePort) {
                    return sc;
                }
            }

            return null;

        } finally {
            lk.unlock();
        }
    }
}
