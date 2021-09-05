package com.example.demo;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
/**
 *
 * @author zhangliang
 *
 * 2016年4月8日 下午5:48:54
 */
public class ConTest {

    final Lock lock = new ReentrantLock();
    final Condition condition = lock.newCondition();

    public static void main(String[] args) {
        // TODO Auto-generated method stub
        ConTest test = new ConTest();
        Producer producer = test.new Producer();
        Consumer consumer = test.new Consumer();

        producer.start();
        consumer.start();
    }

    class Consumer extends Thread {
        @Override
        public void run() {
            consume();
        }
        private void consume() {
            try {
                lock.lock();
                System.out.println("我在等一个新信号" + Thread.currentThread().getName());
                lock.unlock();
                condition.await();
                lock.lock();
            } catch (InterruptedException e) {
                e.printStackTrace();
            } finally {
                System.out.println("拿到一个信号" + Thread.currentThread().getName());
                lock.unlock();
            }
        }
    }

    class Producer extends Thread {
        @Override
        public void run() {
            produce();
        }

        private void produce() {
            try {
                lock.lock();
                System.out.println("我拿到锁" + Thread.currentThread().getName());
                condition.signalAll();
//                condition.signal();
                System.out.println("我发出了一个信号：" + Thread.currentThread().getName());
            } finally {
                lock.unlock();
            }
        }
    }

}
