package worker;

import org.apache.log4j.Logger;

/**
 * Created by kaze on 16-7-14.
 * 抽象线程池的单位线程执行类
 */
@SuppressWarnings("all")
public abstract class AbstractEventWorkerRunner implements Runnable {

    private final static Logger logger = Logger.getLogger(AbstractEventWorkerRunner.class);

    protected EventWorker eventWorker;

    public void setEventWorker(EventWorker eventWorker) {
        this.eventWorker = eventWorker;
    }

    @Override
    public void run() {
        Long delay;
        try {
            if (eventWorker != null) {
                if (eventWorker.isLoop()) {
                    try {
                        while (eventWorker.isRunning()) {
                            if ((delay = eventWorker.getDelay()) != null) {
                                Thread.sleep(delay);
                            }
                            eventWorker.doRun();
                        }
                    } catch (InterruptedException e) {
                        logger.error("error", e);
                    }
                } else {
                    if (eventWorker.isRunning()) {
                        try {
                            if ((delay = eventWorker.getDelay()) != null) {
                                Thread.sleep(delay);
                            }
                            eventWorker.doRun();
                        } catch (InterruptedException e) {
                            logger.error("error", e);
                        }
                    }
                }
            }
        } finally {
            afterRun();
        }
    }

    /**
     * 执行完之后的动作
     */
    public abstract void afterRun();

    /**
     * 停止线程工作
     */
    public void stop() {
        eventWorker.stop();
    }

}
