package com.waldener.eventbus;

/**
 * Created by waldener on 2018/10/15.
 */
class CategorySubscription {
    final Object subscriber;
    final CategorySubscriberMethod categorySubscriberMethod;

    volatile boolean active;

    CategorySubscription(Object subscriber, CategorySubscriberMethod categorySubscriberMethod) {
        this.subscriber = subscriber;
        this.categorySubscriberMethod = categorySubscriberMethod;
        active = true;
    }
}
