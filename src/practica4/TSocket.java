package practica4;

import practica1.CircularQ.CircularQueue;
import util.Const;
import util.TCPSegment;
import util.TSocket_base;

public class TSocket extends TSocket_base {

    //sender variable:
    protected int MSS;

    //receiver variables:
    protected CircularQueue<TCPSegment> rcvQueue;
    protected int rcvSegConsumedBytes;

    protected TSocket(Protocol p, int localPort, int remotePort) {
        super(p.getNetwork());
        this.localPort = localPort;
        this.remotePort = remotePort;
        p.addActiveTSocket(this);
        MSS = network.getMTU() - Const.IP_HEADER - Const.TCP_HEADER;
        rcvQueue = new CircularQueue<>(Const.RCV_QUEUE_SIZE);
        rcvSegConsumedBytes = 0;
    }

    @Override
    public void sendData(byte[] data, int offset, int length) {

        int sent = 0;
        while (sent < length) {

            int chunkSize = Math.min(MSS, length - sent);
            TCPSegment seg = segmentize(data, offset + sent, chunkSize);
            network.send(seg);
            printSndSeg(seg);
            sent += chunkSize;
        }
    }

    protected TCPSegment segmentize(byte[] data, int offset, int length) {

        TCPSegment seg = new TCPSegment();
        seg.setData(data, offset, length);
        seg.setPsh(true);
        seg.setSourcePort(localPort);
        seg.setDestinationPort(remotePort);
        return seg;
    }

    @Override
    public int receiveData(byte[] buf, int offset, int length) {
        lock.lock();
        try {

            while (rcvQueue.empty()) {

                appCV.awaitUninterruptibly();
            }

            return this.consumeSegment(buf, offset, length);
        } finally {
            lock.unlock();
        }
    }

    protected int consumeSegment(byte[] buf, int offset, int length) {
        TCPSegment seg = rcvQueue.peekFirst();
        int a_agafar = Math.min(length, seg.getDataLength() - rcvSegConsumedBytes);
        System.arraycopy(seg.getData(), rcvSegConsumedBytes, buf, offset, a_agafar);
        rcvSegConsumedBytes += a_agafar;
        if (rcvSegConsumedBytes == seg.getDataLength()) {
            rcvQueue.get();
            rcvSegConsumedBytes = 0;
        }
        return a_agafar;
    }

    protected void sendAck() {

        TCPSegment ack = new TCPSegment();
        ack.setAck(true);
        ack.setSourcePort(localPort);
        ack.setDestinationPort(remotePort);
        network.send(ack);
        printSndSeg(ack);
    }

    @Override
    public void processReceivedSegment(TCPSegment rseg) {

        lock.lock();
        try {

            printRcvSeg(rseg);

            if (rseg.isAck()) {
                //nothing to be done in this exercise.
                return;
            }

            if (!rcvQueue.full()) {
                rcvQueue.put(rseg);
                appCV.signalAll();
                this.sendAck();
            } else {
                    log.printRED("processReceivedSegment: rcvQueue plena, segment descartat: " + rseg);
            }
        } finally {
            lock.unlock();
        }
    }

}
