package practica2.P0CZ.Monitor;

public class TestSumCZ {

    public static void main(String[] args) throws InterruptedException {
        
        MonitorCZ mon = new MonitorCZ();
        CounterThreadCZ p1 = new CounterThreadCZ(mon);
        CounterThreadCZ p2 = new CounterThreadCZ(mon);
        
        p1.start();
        p2.start();
        
        p1.join();
        p2.join();
        
        System.out.println("Valor de x: " + mon.getX());
    }
}
