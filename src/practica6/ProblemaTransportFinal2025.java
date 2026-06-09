package practica6;

import java.util.HashMap;
import java.util.Map;
import practica1.CircularQ.CircularQueue;
import practica4.Protocol;
import util.Const;
import util.TCPSegment;
import util.TSocket_base;

/**
 *
 * @author franc
 */
public class ProblemaTransportFinal2025 extends TSocket_base {

    // Sender variables
    protected int MSS, snd_sndNxt, snd_rcvNxt, snd_rcvWnd, snd_cngWnd_init, snd_minWnd;
    protected double snd_cngWnd;
    protected boolean zero_wnd_probe_ON;

    // Reciever variables
    protected int rcv_rcvNxt;
    protected CircularQueue<TCPSegment> rcv_Queue;
    protected Map<Integer, TCPSegment> out_of_order_segs;
    protected int rcv_SegConsumedBytes;

    //Altres
    protected int snd_num_ack_duplicados;
    protected Map<Integer, TCPSegment> snd_seg_pendents_ack;

    protected ProblemaTransportFinal2025(Protocol p, int localPort, int remotePort) {

        super(p.getNetwork());
        this.localPort = localPort;
        this.remotePort = remotePort;
        p.addActiveTSocket(this);

        MSS = p.getNetwork().getMTU() - Const.IP_HEADER - Const.TCP_HEADER;
        MSS = 10;
        this.snd_rcvWnd = Const.RCV_QUEUE_SIZE;
        this.snd_cngWnd_init = 8;

        this.snd_cngWnd = snd_cngWnd_init;
        this.snd_minWnd = Math.min(snd_rcvWnd, (int) snd_cngWnd);

        this.rcv_Queue = new CircularQueue<>(Const.RCV_QUEUE_SIZE);
        this.out_of_order_segs = new HashMap<>();
        this.snd_seg_pendents_ack = new HashMap<>();
    }

    // Sender part
    @Override
    public void sendData(byte[] data, int offset, int length) {

        lock.lock();
        try {

            int sent = 0;
            while (sent < length) {

                while (snd_sndNxt - snd_rcvNxt >= snd_minWnd) {

                    appCV.awaitUninterruptibly();
                }

                int a_posar = 1;
                if (snd_rcvWnd > 0) {

                    a_posar = Math.min(MSS, length - sent);
                }

                TCPSegment seg = segmentize(data, offset + sent, a_posar);
                this.snd_seg_pendents_ack.put(seg.getSeqNum(), seg); //Modificación 1

                if (snd_rcvWnd > 0) {

                    network.send(seg);
                } else {

                    zero_wnd_probe_ON = true;
                    log.printPURPLE("----- Zero-Window probe ON ------");
                }

                startRTO(seg);
                snd_sndNxt++;
                sent += a_posar;
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

            if (seg.getSeqNum() >= snd_sndNxt) {

                if (zero_wnd_probe_ON) {

                    log.printPURPLE("0−wnd probe : " + seg);

                } else {

                    log.printPURPLE("retrans: " + seg);
                }
                network.send(seg);
                startRTO(seg);
                //Modificación 2
                if (!zero_wnd_probe_ON && seg.getSeqNum() == snd_rcvNxt) {
                    snd_cngWnd = Math.max(snd_cngWnd_init, (int) (snd_cngWnd / 2));
                    snd_minWnd = Math.max(1, Math.min(snd_rcvWnd, (int) snd_cngWnd));
                }
            } else {

                System.out.println("sender - Segment NO reenviat: " + seg.getSeqNum());
                snd_seg_pendents_ack.remove(seg.getSeqNum());
            }
        } finally {

            lock.unlock();
        }
    }

    //Receiver part
    @Override
    public int receiveData(byte[] data, int offset, int maxlen) {

        lock.lock();
        try {

            while (rcv_Queue.empty()) {

                appCV.awaitUninterruptibly();
            }

            int agafats = 0;
            while (agafats < maxlen && !rcv_Queue.empty()) {

                agafats += this.consumeSegment(data, offset + agafats, maxlen - agafats);
            }

            return agafats;
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
        ack.setSeqNum(rcv_rcvNxt);
        ack.setWnd(this.rcv_Queue.free());

        network.send(ack);
    }

    @Override
    public void processReceivedSegment(TCPSegment rseg) {

        lock.lock();
        try {

            printRcvSeg(rseg);

            if (rseg.isAck()) {

                if (rseg.getSeqNum() == snd_rcvNxt) {

                    this.snd_num_ack_duplicados++;
                    if (snd_num_ack_duplicados == 3) {

                        System.err.println("sender − segment fast−retransmit : " + rseg.getAckNum());
                        network.send(this.snd_seg_pendents_ack.get(rseg.getAckNum()));
                    }
                } else if (rseg.getSeqNum() > snd_rcvNxt) {

                    this.snd_num_ack_duplicados = 0;

                    if (zero_wnd_probe_ON) {

                        this.zero_wnd_probe_ON = false;
                        log.printPURPLE("----- zero−window probe OFF -----");
                    }

                    for (int i = 0; i < rseg.getAckNum() - snd_rcvNxt; i++) {
                        snd_cngWnd += 1.0 / ((int) snd_cngWnd); // += 1/cwnd por segmento
                    }

                    snd_rcvNxt = rseg.getSeqNum();
                    snd_rcvWnd = rseg.getWnd();
                    snd_minWnd = Math.max(1, Math.min(snd_rcvWnd, (int) snd_cngWnd));
                    appCV.signal();
                }

            } else if (rseg.isPsh() && !rcv_Queue.full()) {

                if (rseg.getSeqNum() > rcv_rcvNxt) {

                    out_of_order_segs.put(rseg.getSeqNum(), rseg);
                    System.out.println("\t\t\t\t\t\t\t\treceiver − guardat fora d’ordre : " + rseg.getSeqNum());

                } else {

                    if (rseg.getSeqNum() == rcv_rcvNxt) {

                        rcv_Queue.put(rseg);
                        System.out.println("\t\t\t\t\t\t\t\treceiver − introduit el : " + rcv_rcvNxt);
                        rcv_rcvNxt++;
                        appCV.signal();

                        while (out_of_order_segs.containsKey(rcv_rcvNxt) && !rcv_Queue.full()) {

                            rcv_Queue.put(out_of_order_segs.remove(rcv_rcvNxt));
                            System.out.println("\t\t\t\t\t\t\t\treceiver − introduit en ordre : " + rcv_rcvNxt);
                            rcv_rcvNxt++;
                        }
                    }

                    sendAck();
                }
            }
        } finally {

            lock.unlock();
        }
    }

}
