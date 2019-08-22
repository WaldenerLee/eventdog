package com.waldener.eventbus;

import org.greenrobot.eventbus.SubscriberMethod;
import org.greenrobot.eventbus.ThreadMode;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Created by Waldener on 2018/10/13.
 */
class CategorySubscriberMethod {
    private final String category;
    private final SubscriberMethod subscriberMethod;
    private volatile Method method;
    private volatile ThreadMode threadMode;
    private volatile int priority;
    private volatile boolean sticky;

    CategorySubscriberMethod(String category, SubscriberMethod subscriberMethod) {
        this.category = category;
        this.subscriberMethod = subscriberMethod;

        try {
            Class<?> clazz = subscriberMethod.getClass();

            Field methodField = clazz.getDeclaredField("method");
            methodField.setAccessible(true);
            this.method = (Method) methodField.get(subscriberMethod);

            Field threadModeField = clazz.getDeclaredField("threadMode");
            threadModeField.setAccessible(true);
            this.threadMode = (ThreadMode) threadModeField.get(subscriberMethod);

            Field priorityField = clazz.getDeclaredField("priority");
            priorityField.setAccessible(true);
            this.priority = (int) priorityField.get(subscriberMethod);

            Field stickyField = clazz.getDeclaredField("sticky");
            stickyField.setAccessible(true);
            this.sticky = (boolean) stickyField.get(subscriberMethod);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    String getCategory() {
        return category;
    }

    SubscriberMethod getSubscriberMethod() {
        return subscriberMethod;
    }

    Method getMethod() {
        return method;
    }

    ThreadMode getThreadMode() {
        return threadMode;
    }

    int getPriority() {
        return priority;
    }

    boolean isSticky() {
        return sticky;
    }
}
