package com.gm.mqtransfer.provider.facade.timer;

import java.util.function.Supplier;

public class SystemTime implements Time {

	@Override
    public long milliseconds() {
        return System.currentTimeMillis();
    }

    @Override
    public long nanoseconds() {
        return System.nanoTime();
    }

    @Override
    public void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            // just wake up early
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public void waitObject(Object obj, Supplier<Boolean> condition, long deadlineMs) throws InterruptedException {
        synchronized (obj) {
            while (true) {
                if (condition.get())
                    return;

                long currentTimeMs = milliseconds();
                if (currentTimeMs >= deadlineMs)
                    throw new InterruptedException("Condition not satisfied before deadline");

                obj.wait(deadlineMs - currentTimeMs);
            }
        }
    }

}
