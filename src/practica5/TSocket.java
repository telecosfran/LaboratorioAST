package practica5;

import practica1.CircularQ.CircularQueue;
import practica4.Protocol;
import util.Const;
import util.TSocket_base;
import util.TCPSegment;

public class TSocket extends TSocket_base {

    // Sender variables:
    protected int MSS;
    protected int snd_sndNxt;
    protected int snd_rcvWnd;
    protected int snd_rcvNxt;
    protected boolean zero_wnd_probe_ON;

    // Receiver variables:
    protected CircularQueue<TCPSegment> rcv_Queue;
    protected int rcv_SegConsumedBytes;
    protected int rcv_rcvNxt;

    protected TSocket(Protocol p, int localPort, int remotePort) {
        super(p.getNetwork());
        this.localPort = localPort;
        this.remotePort = remotePort;
        p.addActiveTSocket(this);

        // init sender variables
        MSS = p.getNetwork().getMTU() - Const.IP_HEADER - Const.TCP_HEADER;
        snd_rcvWnd = Const.RCV_QUEUE_SIZE;
        snd_sndNxt = 0;
        snd_rcvNxt = 0;
        zero_wnd_probe_ON = false;

        // init receiver variables
        rcv_Queue = new CircularQueue<>(Const.RCV_QUEUE_SIZE);
        //rcv_Queue = new CircularQueue<>(2);
        rcv_SegConsumedBytes = 0;
        rcv_rcvNxt = 0;

    }

    // -------------  SENDER PART  ---------------
    @Override
    public void sendData(byte[] data, int offset, int length) {
        lock.lock();
        try {

            int sent = 0;
            while (sent < length) {

                while (this.snd_sndNxt != this.snd_rcvNxt) {

                    appCV.awaitUninterruptibly();
                }

                if (snd_rcvWnd == 0) {

                    if (!this.zero_wnd_probe_ON) {

                        this.zero_wnd_probe_ON = true;
                        log.printRED("zero-window probe ON");
                    }

                    int chunksize = 1;
                    TCPSegment probe = segmentize(data, offset + sent, chunksize);
                    network.send(probe);
                    log.printPURPLE("0_wnd probe: " + probe);
                    startRTO(probe);

                    snd_sndNxt++;
                    sent += chunksize;
                } else {

                    if (this.zero_wnd_probe_ON) {
                        this.zero_wnd_probe_ON = false;
                        log.printRED("zero-window probe OFF");

                    }
                    int chunkSize = Math.min(MSS, length - sent);
                    TCPSegment seg = segmentize(data, offset + sent, chunkSize);

                    network.send(seg);
                    printSndSeg(seg);
                    startRTO(seg);

                    snd_sndNxt++;
                    sent += chunkSize;
                }
            }
        } finally {
            lock.unlock();
        }
    }

    protected TCPSegment segmentize(byte[] data, int offset, int length) {

        TCPSegment seg = new TCPSegment();
        seg.setData(data, offset, length);
        seg.setPsh(true);
        seg.setSourcePort(localPort);
        seg.setDestinationPort(remotePort);

        seg.setSeqNum(snd_sndNxt);

        return seg;
    }

    @Override
    protected void timeout(TCPSegment seg) {
        lock.lock();
        try {

            if (seg.getSeqNum() >= snd_rcvNxt) {

                log.printPURPLE("retrans: " + seg);
                network.send(seg);
                startRTO(seg);
            }
        } finally {
            lock.unlock();
        }
    }

    // -------------  RECEIVER PART  ---------------
    @Override
    public int receiveData(byte[] buf, int offset, int maxlen) {
        lock.lock();
        try {

            while (rcv_Queue.empty()) {

                appCV.awaitUninterruptibly();
            }

            return this.consumeSegment(buf, offset, maxlen);
        } finally {
            lock.unlock();
        }
    }

    protected int consumeSegment(byte[] buf, int offset, int length) {
        TCPSegment seg = rcv_Queue.peekFirst();
        int a_agafar = Math.min(length, seg.getDataLength() - rcv_SegConsumedBytes);
        System.arraycopy(seg.getData(), rcv_SegConsumedBytes, buf, offset, a_agafar);
        rcv_SegConsumedBytes += a_agafar;
        if (rcv_SegConsumedBytes == seg.getDataLength()) {
            rcv_Queue.get();
            rcv_SegConsumedBytes = 0;
        }
        return a_agafar;
    }

    protected void sendAck() {

        TCPSegment ack = new TCPSegment();
        ack.setAck(true);
        ack.setSourcePort(localPort);
        ack.setDestinationPort(remotePort);

        ack.setAckNum(rcv_rcvNxt);
        ack.setWnd(rcv_Queue.free());

        network.send(ack);
        printSndSeg(ack);
    }

    // -------------  SEGMENT ARRIVAL  -------------
    @Override
    public void processReceivedSegment(TCPSegment rseg) {
        lock.lock();
        try {

            printRcvSeg(rseg);

            if (rseg.isPsh()) {

                if (!rcv_Queue.full()) {

                    if (rseg.getSeqNum() == rcv_rcvNxt) {

                        rcv_Queue.put(rseg);
                        rcv_rcvNxt++;
                        appCV.signal();
                    }
                }
                sendAck();

            }

            if (rseg.isAck()) {

                if (rseg.getAckNum() > snd_rcvNxt) {

                    snd_rcvNxt = rseg.getAckNum();
                }

                snd_rcvWnd = rseg.getWnd();
                appCV.signalAll();

            }

        } finally {
            lock.unlock();
        }
    }
}
