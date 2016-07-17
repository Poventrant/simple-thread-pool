package worker;

/**
 * Created by kaze on 16-7-14.
 * 抽象线程池中的单位类
 */
public abstract class EventWorker {

    //是否执行
    private volatile boolean running = true;
    //是否循环
    private boolean loop = false;
    //每次执行之前的延迟
    private Long delay;

    public abstract void doRun();

    public void setRunning(boolean running) {
        this.running = running;
    }

    public void setLoop(boolean loop) {
        this.loop = loop;
    }

    public void setDelay(Long delay) {
        this.delay = delay;
    }

    public boolean isLoop() {
        return loop;
    }

    public Long getDelay() {
        return delay;
    }

    public boolean isRunning() {
        return running;
    }

    public void stop() {
        running  = false;
    }

    public void start() {
        running = true;
    }
}
