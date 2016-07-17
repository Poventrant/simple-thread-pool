package test;

import worker.EventWorker;
import worker.EventWorkerExecutor;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * 单元测试，测试redis的函数线程安全性
 */
public class WorkerTest {

    static AtomicInteger count = new AtomicInteger(0);

    public static void main(String[] args) throws InterruptedException {
        int size = 500;
        EventWorkerExecutor eventWorkerExecutor = new EventWorkerExecutor();
        WorkerTest workerTest = new WorkerTest();
        EventWorker eventWorker;
        for (int i = 0; i < size; i++) {
            eventWorker = workerTest.new KazeWorker();
            eventWorker.setDelay(200L);
            eventWorkerExecutor.process(eventWorker);
        }
        Thread.sleep(5000);
        eventWorkerExecutor.shutdown();
    }

    private class KazeWorker extends EventWorker {
        @Override
        public void doRun() {
            System.out.println("hi" + " " + count.incrementAndGet());
        }

    }

}
