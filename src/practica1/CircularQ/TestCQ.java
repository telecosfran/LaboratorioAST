package practica1.CircularQ;

import java.util.Iterator;

public class TestCQ {

    public static void main(String[] args) {
        System.out.println("=== INICIANDO TESTS DE CIRCULAR QUEUE ===");
        
        // Creamos una cola pequeña de capacidad 5 para forzar la circularidad rápido
        CircularQueue<Integer> q = new CircularQueue<>(5);

        System.out.println("\n--- TEST 1: Comportamiento en Vacio ---");
        System.out.println("Esta vacia " + q.empty()); // true
        System.out.println("Espacio libre: " + q.free()); // 5
        System.out.print("Intento de peekFirst: ");
        q.peekFirst(); // Debería imprimir "Queue is empty..." y devolver null
        System.out.print("Intento de get: ");
        q.get();       // Debería imprimir "Queue is empty" y devolver null


        System.out.println("\n--- TEST 2: Llenado hasta el limite ---");
        q.put(10);
        q.put(20);
        q.put(30);
        q.put(40);
        q.put(50);
        System.out.println("Contenido: " + q); // [10, 20, 30, 40, 50]
        System.out.println("Esta llena? " + q.full()); // true
        System.out.println("Tamanyo actual: " + q.size()); // 5
        System.out.print("Intento de put extra: ");
        q.put(60); // Debería imprimir "Queue is full" y no hacer nada


        System.out.println("\n--- TEST 3: Forzando la Circularidad (Wrap-around) ---");
        System.out.println("Hacemos get() -> Sacamos el: " + q.get()); // Saca 10
        System.out.println("Hacemos get() -> Sacamos el: " + q.get()); // Saca 20
        System.out.println("Contenido actual: " + q); // [30, 40, 50]
        System.out.println("Espacio libre ahora: " + q.free()); // 2
        
        System.out.println("Hacemos put(60) y put(70)...");
        q.put(60);
        q.put(70);
        // Ahora internamente el array es [60, 70, 30, 40, 50] pero G apunta al 30
        System.out.println("Contenido tras dar la vuelta: " + q); // [30, 40, 50, 60, 70]


        System.out.println("\n--- TEST 4: Prueba de fuego del Iterador y Remove() ---");
        Iterator<Integer> it = q.iterator();
        
        while (it.hasNext()) {
            Integer valor = it.next();
            System.out.println("Iterador lee: " + valor);
            
            // Vamos a borrar el 50 (que está en medio de la cola, forzando un desplazamiento circular)
            if (valor == 50) {
                System.out.println("  -> Borrando el 50 con el iterador!");
                it.remove();
            }
        }
        System.out.println("Contenido tras borrar el 50: " + q); // [30, 40, 60, 70]
        System.out.println("Tamanyo actual: " + q.size() + ", Libre: " + q.free());


        System.out.println("\n--- TEST 5: Vaciado total usando el Iterador ---");
        // Añadimos un elemento más para llenar el hueco dejado por el 50
        q.put(80);
        System.out.println("Llenamos la cola de nuevo: " + q); // [30, 40, 60, 70, 80]
        
        Iterator<Integer> it2 = q.iterator();
        while (it2.hasNext()) {
            it2.next();
            it2.remove(); // Borra todo uno por uno
        }
        
        System.out.println("Contenido tras vaciar con remove(): " + q); // []
        System.out.println("Esta vacia al final? " + q.empty()); // true
        
        System.out.println("\n=== TESTS FINALIZADOS CON EXITO ===");
    }
}
