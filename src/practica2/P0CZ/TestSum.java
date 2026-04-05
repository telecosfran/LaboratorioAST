package practica2.P0CZ;

public class TestSum {

    public static void main(String[] args) throws InterruptedException {
        
        CounterThread p1 = new CounterThread();        
        CounterThread p2 = new CounterThread();
        
        p1.start();
        p2.start();
        
    }
}
