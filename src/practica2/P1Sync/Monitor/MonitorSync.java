package practica2.P1Sync.Monitor;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class MonitorSync {

    private final int N;
    private final ReentrantLock l = new ReentrantLock();
    private final Condition vc = l.newCondition();
    private int ultim = 1;

    public MonitorSync(int N) {
        this.N = N;
    }

    public void waitForTurn(int id) {
        
        l.lock();
        try{
            while(id == ultim){
            vc.awaitUninterruptibly();
            }
        }finally{
            l.unlock();
        }
    }

    public void transferTurn() {
        
        l.lock();
        try{
          ultim = (ultim + 1) % N;
          vc.signalAll();
        }finally{
            l.unlock();
        }
    }
}
