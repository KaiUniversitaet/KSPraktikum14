package addons.threadtest

// Ein konkurierender Zugriff auf x

class ThreadTest52 {
    int x = 0

    void decrement() {
        int y
        for (i in 1..4) {
            synchronized (x) {
                println("decrement begin: x=$x")
                y = x
                sleep(400)
                y -= 1
                x = y
                println("decrement ende: x=$x")
            }
        }
    }

    void increment() {
        int y
        synchronized (x) {
            println("increment begin: x=$x")
            y = x
            y += 1
            sleep(2000)
            x = y
            println("increment ende: x=$x")
        }
    }
}

def klasse = new ThreadTest52()

Thread t1 = new Thread({klasse.increment()})
Thread t2 = new Thread({klasse.decrement()})

t1.start()
t2.start()

t1.join()
t2.join()
