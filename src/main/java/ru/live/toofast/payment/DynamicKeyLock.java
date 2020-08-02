package ru.live.toofast.payment;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Adaptation of https://stackoverflow.com/a/50468304
 */
public class DynamicKeyLock<T> {
    private final ConcurrentHashMap<T, LockAndCounter> locksMap;

    public DynamicKeyLock() {
        this.locksMap = new ConcurrentHashMap<>();
    }

    private static class LockAndCounter {
        private final Lock lock = new ReentrantLock();
        private final AtomicInteger counter = new AtomicInteger(0);
    }

    private LockAndCounter getLock(T key) {
        return locksMap.compute(key, (key1, lockAndCounterInner) ->
        {
            if (lockAndCounterInner == null) {
                lockAndCounterInner = new LockAndCounter();
            }
            lockAndCounterInner.counter.incrementAndGet();
            return lockAndCounterInner;
        });
    }

    private void cleanupLock(T key, LockAndCounter lockAndCounterOuter) {
        if (lockAndCounterOuter.counter.decrementAndGet() == 0) {
            locksMap.compute(key, (key1, lockAndCounterInner) ->
            {
                if (lockAndCounterInner == null || lockAndCounterInner.counter.get() == 0) {
                    return null;
                }
                return lockAndCounterInner;
            });
        }
    }

    public void lock(T key) {
        LockAndCounter lockAndCounter = getLock(key);
        lockAndCounter.lock.lock();
    }

    public void unlock(T key) {
        LockAndCounter lockAndCounter = locksMap.get(key);
        lockAndCounter.lock.unlock();

        cleanupLock(key, lockAndCounter);
    }
}