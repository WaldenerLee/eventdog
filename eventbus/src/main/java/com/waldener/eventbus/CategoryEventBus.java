package com.waldener.eventbus;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.SubscriberMethod;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Created by Waldener on 2018/10/13.
 */
public class CategoryEventBus {
    private static final String TAG = CategoryEventBus.class.getSimpleName();
    private static volatile CategoryEventBus defaultInstance;
    private final EventBus eventBus;
    private final CategorySubscriberMethodFinder categorySubscriberMethodFinder;
    private final Map<String, CopyOnWriteArrayList<CategorySubscription>> subscriptionsByEventCategory;
    private final Map<Object, List<String>> categoriesBySubscriber;

    private final ThreadLocal<CategoryPostingThreadState> currentCategoryPostingThreadState = new ThreadLocal<CategoryPostingThreadState>() {
        @Override
        protected CategoryPostingThreadState initialValue() {
            return new CategoryPostingThreadState();
        }
    };

    private final Handler mainHandler;
    private final Handler backgroundHandler;

    private CategoryEventBus(EventBus eventBus) {
        this.eventBus = eventBus;
        categorySubscriberMethodFinder = new CategorySubscriberMethodFinder(eventBus);
        subscriptionsByEventCategory = new HashMap<>();
        categoriesBySubscriber = new HashMap<>();
        mainHandler = new Handler(Looper.getMainLooper());
        HandlerThread backgroundThread = new HandlerThread("background");
        backgroundThread.start();
        backgroundHandler = new Handler(backgroundThread.getLooper());
    }

    public static CategoryEventBus getDefault(){
        if(defaultInstance == null){
            synchronized (CategoryEventBus.class){
                if(defaultInstance == null){
                    defaultInstance = new CategoryEventBus(EventBus.getDefault());
                }
            }
        }
        return defaultInstance;
    }

    private boolean isCategoryRegistered(Object subscriber){
        return categoriesBySubscriber.containsKey(subscriber);
    }

    public void register(Object subscriber){
        if(eventBus.isRegistered(subscriber) || isCategoryRegistered(subscriber)){
            return;
        }
        Class<?> subscriberClass = subscriber.getClass();
        List<SubscriberMethod> subscriberMethods = categorySubscriberMethodFinder.findCategorySubscriberMethods(subscriberClass);
        synchronized (this) {
            for (SubscriberMethod subscriberMethod : subscriberMethods){
                subscribe(subscriber, subscriberMethod);
            }
        }
    }

    public void unregister(Object subscriber){
        if(eventBus.isRegistered(subscriber)){
            eventBus.unregister(subscriber);
        }
        if(isCategoryRegistered(subscriber)){
            List<String> subscribedCategories = categoriesBySubscriber.get(subscriber);
            if(subscribedCategories != null){
                for (String category : subscribedCategories){
                    unsubscribeByCategory(subscriber, category);
                }
                categoriesBySubscriber.remove(subscriber);
            }
        }
    }

    public void post(Object event){
        if(event != null){
            eventBus.post(event);
        }
    }

    public void postSticky(Object event){
        if(event != null){
            eventBus.postSticky(event);
        }
    }

    public void post(String category, Object event){
        if(category != null && !category.isEmpty()){
            categoryPost(new CategoryEvent(category, event));
        }
    }

    public void postSticky(String category, Object event){
        if(category != null && !category.isEmpty()){
            //todo
        }
    }

    @SuppressWarnings("unchecked")
    private void subscribe(final Object subscriber, final SubscriberMethod subscriberMethod){
        try {
            Field methodField = subscriberMethod.getClass().getDeclaredField("method");
            methodField.setAccessible(true);
            Method method = (Method) methodField.get(subscriberMethod);
            Category categoryAnnotation = method.getAnnotation(Category.class);
            if(categoryAnnotation == null){
                Class<?> eventBusClass = eventBus.getClass();
                Method subscribeMethod = eventBusClass.getDeclaredMethod("subscribe", Object.class, SubscriberMethod.class);
                subscribeMethod.setAccessible(true);
                subscribeMethod.invoke(eventBus, subscriber, subscriberMethod);
            }else {
                String category = categoryAnnotation.value();
                CategorySubscriberMethod categorySubscriberMethod = new CategorySubscriberMethod(category, subscriberMethod);
                CategorySubscription categorySubscription = new CategorySubscription(subscriber, categorySubscriberMethod);
                CopyOnWriteArrayList<CategorySubscription> categorySubscriptions = subscriptionsByEventCategory.get(category);
                if(categorySubscriptions == null){
                    categorySubscriptions = new CopyOnWriteArrayList<>();
                    subscriptionsByEventCategory.put(category, categorySubscriptions);
                }else{
                    if(categorySubscriptions.contains(categorySubscription)){
                        throw new JEventBusException("Subscriber " + subscriber.getClass() + " already registered to event " + category);
                    }
                }
                int size = categorySubscriptions.size();
                for (int i = 0; i <= size; i++) {
                    if(i == size || categorySubscriberMethod.getPriority() > categorySubscriptions.get(i).categorySubscriberMethod.getPriority()){
                        categorySubscriptions.add(i, categorySubscription);
                        break;
                    }
                }

                List<String> subscribedCategories = categoriesBySubscriber.get(subscriber);
                if(subscribedCategories == null){
                    subscribedCategories = new ArrayList<>();
                    categoriesBySubscriber.put(subscriber, subscribedCategories);
                }
                subscribedCategories.add(category);

                if(categorySubscriberMethod.isSticky()){
                    Class<?> eventBusClass = eventBus.getClass();
                    Field eventInheritanceField = eventBusClass.getDeclaredField("eventInheritance");
                    eventInheritanceField.setAccessible(true);
                    boolean eventInheritance = (boolean) eventInheritanceField.get(eventBus);
                    if(eventInheritance){
                        //todo
                    }else{
                        //todo
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void unsubscribeByCategory(Object subscriber, String category){
        List<CategorySubscription> categorySubscriptions = subscriptionsByEventCategory.get(category);
        if(categorySubscriptions != null){
            int size = categorySubscriptions.size();
            for (int i = 0; i < size; i++) {
                CategorySubscription categorySubscription = categorySubscriptions.get(i);
                if(categorySubscription.subscriber == subscriber){
                    categorySubscription.active = false;
                    categorySubscriptions.remove(i);
                    i--;
                    size--;
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void categoryPost(@NonNull final CategoryEvent event){
        CategoryPostingThreadState postingState = currentCategoryPostingThreadState.get();
        List<CategoryEvent> eventQueue = postingState.eventQueue;
        eventQueue.add(event);

        try {
            if (!postingState.isPosting) {
                Class<?> eventBusClass = eventBus.getClass();
                Method isMainThreadMethod = eventBusClass.getDeclaredMethod("isMainThread");
                isMainThreadMethod.setAccessible(true);
                boolean isMainThread = (boolean) isMainThreadMethod.invoke(eventBus);
                postingState.isMainThread = isMainThread;
                postingState.isPosting = true;
                if (postingState.canceled) {
                    throw new JEventBusException("Internal error. Abort state was not reset");
                }
                try {
                    while (!eventQueue.isEmpty()) {
                        postSingleCategoryEvent(eventQueue.remove(0), postingState);
                    }
                } finally {
                    postingState.isPosting = false;
                    postingState.isMainThread = false;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void postSingleCategoryEvent(CategoryEvent event, CategoryPostingThreadState postingState){
        try {
            String category = event.getCategory();
            boolean subscriptionFound = postSingleEventForCategory(event, postingState);
            if (!subscriptionFound) {
                Log.e(TAG, "No subscribers registered for event category " + category);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private boolean postSingleEventForCategory(CategoryEvent event, CategoryPostingThreadState postingState){
        CopyOnWriteArrayList<CategorySubscription> categorySubscriptions;
        synchronized (this) {
            categorySubscriptions = subscriptionsByEventCategory.get(event.getCategory());
        }
        if (categorySubscriptions != null && !categorySubscriptions.isEmpty()) {
            for (CategorySubscription categorySubscription : categorySubscriptions){
                postingState.event = event;
                postingState.subscription = categorySubscription;
                boolean aborted = false;
                try {
                    postToCategorySubscription(categorySubscription, event.getEvent(), postingState.isMainThread);
                    aborted = postingState.canceled;
                } catch (Exception e){
                    e.printStackTrace();
                } finally {
                    postingState.event = null;
                    postingState.subscription = null;
                    postingState.canceled = false;
                }
                if(aborted){
                    break;
                }
            }
            return true;
        }
        return false;
    }

    @SuppressWarnings("unchecked")
    private void postToCategorySubscription(CategorySubscription categorySubscription, Object event, boolean isMainThread){
        switch (categorySubscription.categorySubscriberMethod.getThreadMode()){
            case POSTING:
                invokeSubscriber(categorySubscription, event);
                break;
            case MAIN:
            case MAIN_ORDERED:
                if (isMainThread) {
                    invokeSubscriber(categorySubscription, event);
                } else {
                    mainHandler.post(() -> invokeSubscriber(categorySubscription, event));
                }
                break;
            case BACKGROUND:
            case ASYNC:
                if (isMainThread) {
                    backgroundHandler.post(() -> invokeSubscriber(categorySubscription, event));
                } else {
                    invokeSubscriber(categorySubscription, event);
                }
                break;
            default:
                throw new IllegalStateException("Unknown thread mode: " + categorySubscription.categorySubscriberMethod.getThreadMode());
        }
    }

    private void invokeSubscriber(CategorySubscription categorySubscription, Object event){
        try {
            Method method = categorySubscription.categorySubscriberMethod.getMethod();
            method.setAccessible(true);
            method.invoke(categorySubscription.subscriber, event);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
    }

    final static class CategoryPostingThreadState {
        final List<CategoryEvent> eventQueue = new ArrayList<>();
        boolean isPosting;
        boolean isMainThread;
        CategorySubscription subscription;
        CategoryEvent event;
        boolean canceled;
    }
}
