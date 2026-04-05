package practica2.Protocol;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import practica1.CircularQ.CircularQueue;
import util.Const;
import util.TCPSegment;
import util.SimNet;

public class SimNet_Monitor implements SimNet {

    protected CircularQueue<TCPSegment> queue;
    private final ReentrantLock l = new ReentrantLock();
    private final Condition vc = l.newCondition();

    public SimNet_Monitor() {
        queue = new CircularQueue<>(Const.SIMNET_QUEUE_SIZE);
        //Completar
    }

    @Override
    public void send(TCPSegment seg) {
        
        l.lock();
        try{
            while(queue.full()){
                vc.awaitUninterruptibly();
            }
            queue.put(seg);
            vc.signalAll();
        }finally{
            l.unlock();
        }
    }

    @Override
    public TCPSegment receive() {
        l.lock();
        try{
            while(queue.empty()){
                vc.awaitUninterruptibly();
            }
            TCPSegment obj = queue.get();
            vc.signalAll();
            return obj;
        }finally{
            l.unlock();
        }    
    }

    @Override
    public int getMTU() {
        
        return Const.MTU_ETHERNET;
    }

}
