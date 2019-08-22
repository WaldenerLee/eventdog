package com.waldener.eventbus;


import androidx.annotation.NonNull;

/**
 * Created by Waldener on 2018/10/12.
 */
public class EventProxy {

    /**
     * register
     * @param subscriber
     */
    public static void register(@NonNull Object subscriber){
        CategoryEventBus.getDefault().register(subscriber);
    }

    /**
     * unregister
     * @param subscriber
     */
    public static void unregister(@NonNull Object subscriber){
        CategoryEventBus.getDefault().unregister(subscriber);
    }

    /**
     * post
     * @param event
     */
    public static void post(@NonNull Object event){
        CategoryEventBus.getDefault().post(event);
    }

    /**
     * postSticky
     * @param event
     */
    public static void postSticky(@NonNull Object event){
        CategoryEventBus.getDefault().postSticky(event);
    }

    /**
     * post
     * @param category
     * @param event
     */
    public static void post(@NonNull String category, Object event){
        CategoryEventBus.getDefault().post(category, event);
    }

    /**
     * postSticky
     * @param category
     * @param event
     */
    public static void postSticky(@NonNull String category, Object event){
        CategoryEventBus.getDefault().postSticky(category, event);
    }

}
