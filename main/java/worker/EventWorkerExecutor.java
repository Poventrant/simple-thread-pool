package worker;

import org.apache.log4j.Logger;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.*;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Created by kaze on 16-7-14.
 */
public class EventWorkerExecutor {

    private final static Logger logger = Logger.getLogger(EventWorkerExecutor.class);

    private int maxAvailabelThreads = 50;
    private int maxPoolSize = 50;
    private int minSpareThreads = 5;
    private long maxIdleTime = 60000L;

    private final ReentrantLock lock = new ReentrantLock();
    private final Condition condition = lock.newCondition();

    //空闲可用的线程
    private Queue<AbstractEventWorkerRunner> availabelQueue;
    //正在执行的线程
    private final Set<AbstractEventWorkerRunner> runningThread;
    private final BlockingQueue<Runnable> blockingQueue;
    //线程池执行器
    private final ThreadPoolExecutor executor;

    public EventWorkerExecutor() {
        availabelQueue = new LinkedList<>();
        runningThread = new HashSet<>();
        blockingQueue = new LinkedBlockingQueue<>();
        executor = new ThreadPoolExecutor(minSpareThreads, maxPoolSize, maxIdleTime, TimeUnit.MILLISECONDS, blockingQueue);
    }

    /**
     * 开始执行任务
     *
     * @param eventWorker
     */
    public void process(EventWorker eventWorker) {
        try {
            AbstractEventWorkerRunner eventWorkerRunner;
            final ReentrantLock lock = this.lock;
            lock.lock();
            try {
                if (availabelQueue.size() == 0) {
                    if (runningThread.size() < maxAvailabelThreads) {
                        eventWorkerRunner = this.new EventWorkerRunner();
                    } else {
                        while ((eventWorkerRunner = availabelQueue.poll()) == null) {
                            condition.await();
                        }
                    }
                } else {
                    eventWorkerRunner = availabelQueue.poll();
                }
                runningThread.add(eventWorkerRunner);
            } finally {
                lock.unlock();
            }
            try {
                eventWorkerRunner.setEventWorker(eventWorker);
                // 因为eventWorker.start 是volatile，保证可见性
                // 不会发生在executor.execute之后，所以是线程安全的
                eventWorker.start();
                executor.execute(eventWorkerRunner);
            } catch (RejectedExecutionException e) {
                lock.lock();
                try {
                    availabelQueue.add(eventWorkerRunner);
                    runningThread.remove(eventWorkerRunner);
                    condition.signalAll();
                } finally {
                    lock.unlock();
                }
                throw e;
            }
        } catch (Exception e) {
            logger.error("error", e);
        }
    }

    private class EventWorkerRunner extends AbstractEventWorkerRunner {
        private final Logger logger = Logger.getLogger(EventWorkerRunner.class);

        @Override
        public void afterRun() {
            lock.lock();
            try {
                availabelQueue.add(this);
                runningThread.remove(this);
                condition.signalAll();
            } finally {
                lock.unlock();
            }
        }
    }

    /**
     * 关闭线程池
     *
     * @param timeout
     */
    public void shutdown(long timeout) {
        //对runningThread和availabelQueue操作可能会出现冲突，需要加锁
        try {
            lock.lock();
            try {
                for (AbstractEventWorkerRunner eventWorkerRunner : runningThread) {
                    eventWorkerRunner.stop();
                }
            } finally {
                lock.unlock();
            }
            executor.shutdown();
            if (executor.awaitTermination(timeout, TimeUnit.MILLISECONDS)) {
                lock.lock();
                try {
                    availabelQueue.clear();
                    runningThread.clear();
                } finally {
                    lock.unlock();
                }
                logger.info("shutdown...");
            }
        } catch (Exception e) {
            logger.error("error", e);
        }
    }

    public void shutdown() {
        shutdown(1000L);
    }


    public void setMaxAvailabelThreads(int maxAvailabelThreads) {
        this.maxAvailabelThreads = maxAvailabelThreads;
    }

    public void setMaxPoolSize(int maxPoolSize) {
        this.maxPoolSize = maxPoolSize;
    }

    public void setMinSpareThreads(int minSpareThreads) {
        this.minSpareThreads = minSpareThreads;
    }

    public void setMaxIdleTime(long maxIdleTime) {
        this.maxIdleTime = maxIdleTime;
    }

}
