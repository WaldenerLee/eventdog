package com.waldener.eventbus;

/**
 * Created by Waldener on 2018/10/13.
 */
class CategoryEvent {
    private String category;
    private Object event;

    CategoryEvent(String category, Object event) {
        this.category = category;
        this.event = event;
    }

    String getCategory() {
        return category;
    }

    Object getEvent() {
        return event;
    }
}
