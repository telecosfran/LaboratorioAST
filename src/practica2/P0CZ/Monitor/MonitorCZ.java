package practica2.P0CZ.Monitor;

import java.util.concurrent.locks.ReentrantLock;

public class MonitorCZ {

    private int x = 0;
    private final ReentrantLock l = new ReentrantLock();

    public void inc() {

        l.lock();
        try {
            x++;
        } finally {
            l.unlock();
        }
    }

    public int getX() {
        
        l.lock();
        try{
            return x;
        }finally{
            l.unlock();
        }
    }

}
