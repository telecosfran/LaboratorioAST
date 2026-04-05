package practica3;

import util.Const;
import util.TCPSegment;
import util.TSocket_base;
import util.SimNet;

public class TSocketSend extends TSocket_base {

    protected int MSS;       // Maximum Segment Size

    public TSocketSend(SimNet network) {
        super(network);
        MSS = network.getMTU() - Const.IP_HEADER - Const.TCP_HEADER;
    }

    @Override
    public void sendData(byte[] data, int offset, int length) {
        
        int safeLength = Math.min(length, data.length);
        int bytesEnviados = 0;
        
        while (bytesEnviados < safeLength) {

            int chunkSize = Math.min(MSS, safeLength - bytesEnviados);
            
            TCPSegment obj = segmentize(data, offset + bytesEnviados, chunkSize);
            network.send(obj);
            this.printSndSeg(obj);
            bytesEnviados += chunkSize;
        }

    }

    protected TCPSegment segmentize(byte[] data, int offset, int length) {
        
        TCPSegment obj = new TCPSegment();
        obj.setData(data, offset, length);
        obj.setPsh(true);
        return obj;
    }

}
