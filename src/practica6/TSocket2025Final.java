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
public class TSocket2025Final extends TSocket_base {

    // Sender variables
    protected int MSS;
    protected int snd_sndNxt;
    protected int snd_rcvNxt;
    protected int snd_rcvWnd;
    protected int snd_cngWnd;
    protected int snd_minWnd;
    protected boolean zero_wnd_probe_ON;

    // Reciever variables
    protected int rcv_rcvNxt;
    protected CircularQueue<TCPSegment> rcv_Queue;
    protected int rcv_SegConsumedBytes;
    protected Map<Integer, TCPSegment> out_of_order_segs;

    //Constructor
    protected TSocket2025Final(Protocol p, int localPort, int remotePort) {

        super(p.getNetwork());
        this.localPort = localPort;
        this.remotePort = remotePort;
        p.addActiveTSocket(this);

        MSS = p.getNetwork().getMTU() - Const.IP_HEADER - Const.TCP_HEADER;
        MSS = 10;
        snd_rcvWnd = Const.RCV_QUEUE_SIZE;
        snd_cngWnd = 3;
        snd_minWnd = Math.min(snd_rcvWnd, snd_cngWnd);

        rcv_Queue = new CircularQueue<>(Const.RCV_QUEUE_SIZE);
        out_of_order_segs = new HashMap<>();
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

                if (snd_rcvWnd > 0) {

                    network.send(seg);
                } else {

                    zero_wnd_probe_ON = true;
                    log.printPURPLE("----- zero−window probe ON -----");
                }
                startRTO(seg);
                snd_sndNxt++;
                sent += a_posar;
            }
        } finally {
            lock.unlock();
        }
    }

    protected TCPSegment segmentize(byte[] data, int offset, int lenght) {

        TCPSegment seg = new TCPSegment();
        seg.setData(data, offset, lenght);
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

            //Si el timer que expira es el del que espera el receptor, retransmito.
            if (seg.getSeqNum() == snd_rcvNxt) {
                if (zero_wnd_probe_ON) {
                    log.printPURPLE("0−wnd probe : " + seg);
                } else {

                    log.printPURPLE("retrans: " + seg);
                }

                network.send(seg);
                startRTO(seg);

                //Si es de uno posterior, el antiguo tiene prioridad: solo alargo el timer.
            } else if (seg.getSeqNum() > snd_rcvNxt) {

                startRTO(seg);
            }

        } finally {

            lock.unlock();
        }
    }

    // Reciever part
    @Override
    public int receiveData(byte[] buf, int offset, int maxlen) {

        lock.lock();
        try {

            while (this.rcv_Queue.empty()) {

                appCV.awaitUninterruptibly();
            }

            int agafats = 0;
            while (maxlen > agafats && !rcv_Queue.empty()) {

                agafats += this.consumeSegment(buf, offset + agafats, maxlen - agafats);
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
    
    protected void sendAck(){
    
        TCPSegment ack = new TCPSegment();
        ack.setAck(true);
        ack.setSourcePort(localPort);
        ack.setDestinationPort(remotePort);
        ack.setAckNum(rcv_rcvNxt);
        ack.setWnd(rcv_Queue.free());
        
        network.send(ack);
        
    }
    
    @Override
    public void processReceivedSegment(TCPSegment rseg){
    
        lock.lock();
        try{
        
            printRcvSeg(rseg);
            
            if(rseg.isAck() && rseg.getAckNum() >= snd_rcvNxt){
            
                if(zero_wnd_probe_ON){
                
                    zero_wnd_probe_ON = false;
                    log.printPURPLE("----- zero−window probe OFF -----");
                }
                
                snd_rcvNxt = rseg.getAckNum();
                snd_rcvWnd = rseg.getWnd();
                snd_minWnd = Math.max(1, Math.min(snd_rcvWnd, snd_cngWnd));
                appCV.signal();
            }else if(rseg.isPsh() && !rcv_Queue.full()){
            
                if(rseg.getSeqNum() > rcv_rcvNxt){
                
                    out_of_order_segs.put(rseg.getSeqNum(), rseg);
                    System. out . println ("\t\t\t\t\t\t\t\treceiver − guardat fora d’ordre : " + rseg.getSeqNum()); 
                }else{
                
                    if(rseg.getSeqNum() == rcv_rcvNxt){
                    
                        rcv_Queue.put(rseg);
                        System.out.println("\t\t\t\t\t\t\t\treceiver − introduit el : " + rcv_rcvNxt);
                        rcv_rcvNxt++;
                        appCV.signal();
                        
                        // Hay que buscar en los llegados fuera de orden a ver si esta el rcv_rcvNxt
                        while(out_of_order_segs.containsKey(rcv_rcvNxt)){
                        
                            rcv_Queue.put(out_of_order_segs.remove(rcv_rcvNxt));
                            System.out.println("\t\t\t\t\t\t\t\treceiver − introduit en ordre : " + rcv_rcvNxt);
                            rcv_rcvNxt++;
                        }
                    }
                    
                    sendAck();
                }
            }
            
            
        }finally{
        
            lock.unlock();
        }
    }
    

    
}
