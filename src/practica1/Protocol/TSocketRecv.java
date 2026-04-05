package practica1.Protocol;

import util.TCPSegment;
import util.TSocket_base;
import util.SimNet;

public class TSocketRecv extends TSocket_base {

    public TSocketRecv(SimNet network) {
        super(network);
    }

    @Override
    public int receiveData(byte[] data, int offset, int length) {

        TCPSegment s = network.receive();
        byte[] payload = s.getData();
        int payloadLength = s.getDataLength();

        int bytesToCopy = Math.min(payloadLength, length);

        System.arraycopy(payload, 0, data, offset, bytesToCopy);
        
        this.printRcvSeg(s);

        return bytesToCopy;
    }
}
