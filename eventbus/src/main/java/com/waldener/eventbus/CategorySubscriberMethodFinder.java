package com.waldener.eventbus;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.SubscriberMethod;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

/**
 * Created by waldener on 2019/3/28.
 */
class CategorySubscriberMethodFinder {
    private EventBus eventBus;
    private Object reflectObject;
    private Method reflectMethod;

    CategorySubscriberMethodFinder(EventBus eventBus) {
        this.eventBus = eventBus;
    }

    @SuppressWarnings("unchecked")
    List<SubscriberMethod> findCategorySubscriberMethods(Class<?> subscriberClass){
        List<SubscriberMethod> subscriberMethods = null;
        if(reflectObject == null || reflectMethod == null){
            try {
                final EventBus eventBus = this.eventBus;
                final Field field = eventBus.getClass().getDeclaredField("subscriberMethodFinder");
                field.setAccessible(true);
                reflectObject = field.get(eventBus);
                final Class<?> fieldClass = reflectObject.getClass();
                final Method fieldMethod = fieldClass.getDeclaredMethod("findSubscriberMethods", Class.class);
                fieldMethod.setAccessible(true);
                reflectMethod = fieldMethod;
            } catch (NoSuchFieldException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (NoSuchMethodException e) {
                e.printStackTrace();
            }
        }
        if(reflectObject != null && reflectMethod != null){
            try {
                subscriberMethods = (List<SubscriberMethod>) reflectMethod.invoke(reflectObject, subscriberClass);
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            }
        }
        return subscriberMethods;
    }

}
