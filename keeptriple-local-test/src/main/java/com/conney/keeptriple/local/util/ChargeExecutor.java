package com.conney.keeptriple.local.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicReference;

public class ChargeExecutor extends Timer {

    private static final Logger logger = LoggerFactory.getLogger(ChargeExecutor.class);

    /* 要执行的任务引用 */
    private final AtomicReference<ChargeTask> taskReference = new AtomicReference();

    /* 蓄力时间 */
    private long chargeTime;

    /* 蓄力限制 */
    private int chargeLimit;

    public ChargeExecutor(long chargeTime, int chargeLimit) {
        this.chargeTime = chargeTime;
        this.chargeLimit = chargeLimit;
    }

    /**
     * 执行任务: 该方法不会立即执行任务, 只有在蓄力满后才会执行
     * 蓄满条件: 任务蓄满(达到数量)或者蓄力时间达到
     *
     * @param task 任务
     */
    public ChargeTask execute(ChargeTask task) {
        if (taskReference.compareAndSet(null, task.setup(this))) {
            super.schedule(task, chargeTime);
            return task;
        } else {
            ChargeTask ct = taskReference.get();

            if (ct != null) {
                if (ct.charge(task)) {
                    ct.run(true);
                }

                return ct;
            } else {
                return execute(task);
            }
        }
    }

    public static abstract class ChargeTask<T> extends TimerTask {

        protected volatile ChargeExecutor chargeExecutor;

        protected List<T> list;

        private boolean executed = false;

        private int limit;

        /**
         * 装载执行器
         *
         * @param chargeExecutor
         * @return
         */
        public ChargeTask setup(ChargeExecutor chargeExecutor) {
            if (this.chargeExecutor == null) {
                this.chargeExecutor = chargeExecutor;
                this.limit = chargeExecutor.chargeLimit;
                this.list = new ArrayList<>(this.limit);
                this.list.add(getValue());

            }
            return this;
        }

        @Override
        public void run() {
            run(false);
        }

        /**
         * 运行任务
         *  1.从taskReference中找到要执行的任务
         *  2.加锁:停止任务蓄力
         *  2.开始执行
         *  3.释放锁
         *  4.改为已执行状态
         *
         * @param filled 是否蓄满
         */
        private void run(boolean filled) {
            ChargeTask task = chargeExecutor.taskReference.getAndSet(null);

            if (task != null) {
                synchronized (task.list) {
                    try {
                        task.execute(task.list, filled);
                    } catch (Exception e) {
                        logger.error(e.getMessage(), e);
                    } finally {
                        task.executed = true;
                    }
                }
            }
        }

        /**
         * 任务蓄力
         *  1.任务未执行: 将chargeTask加入到taskReference任务的集合, 否则执行chargeTask任务
         *  2.返回是否蓄满
         *
         * @param task
         * @return
         */
        private boolean charge(ChargeTask<T> task) {
            synchronized (list) {
                if (executed) {
                    task.execute(task.list);
                } else {
                    list.addAll(task.list);
                }
            }

            return filled();
        }

        /**
         * 是否蓄满
         *
         * @return
         */
        public boolean filled() {
            return list.size() >= limit;
        }

        /**
         * 执行任务, 由子类实现具体方法
         *
         * @param list 任务集合
         */
        public void execute(List<T> list) {
            execute(list, false);
        }

        /**
         * 设置蓄力列表限制
         *
         * @param limit 限制数值
         */
        public void limit(int limit) {
            this.limit = limit;
        }

        /**
         * 执行任务, 由子类实现具体方法
         *
         * @param list 任务集合
         * @param filled 是否蓄满
         */
        public abstract void execute(List<T> list, boolean filled);

        public abstract T getValue();
    }
}
