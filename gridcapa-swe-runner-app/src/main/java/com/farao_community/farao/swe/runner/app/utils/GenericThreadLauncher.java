/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.swe.runner.app.utils;

import com.farao_community.farao.swe.runner.api.exception.SweInternalException;
import com.farao_community.farao.swe.runner.api.resource.ThreadLauncherResult;
import org.slf4j.MDC;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Theo Pascoli {@literal <theo.pascoli at rte-france.com>}
 */
public class GenericThreadLauncher<T, U> extends Thread {

    private final T threadable;
    private final Method run;
    private final Object[] args;

    private ThreadLauncherResult<U> result;

    public GenericThreadLauncher(T threadable, String id, Object... args) {
        super(id);
        this.run = getMethodAnnotatedWith(threadable.getClass());
        this.threadable = threadable;
        this.args = args;
    }

    private static Method getMethodAnnotatedWith(final Class<?> type) {
        List<Method> methods = getMethodsAnnotatedWith(type);
        if (methods.isEmpty()) {
            throw new SweInternalException("the class " + type.getCanonicalName() + " does not have his running method annotated with @Threadable");
        } else if (methods.size() > 1) {
            throw new SweInternalException("the class " + type.getCanonicalName() + " must have only one method annotated with @Threadable");
        } else {
            return methods.get(0);
        }
    }

    private static List<Method> getMethodsAnnotatedWith(final Class<?> type) {
        final List<Method> methods = new ArrayList<>();
        Class<?> klass = type;
        while (klass != Object.class) { // need to traverse a type hierarchy in order to process methods from super types
            // iterate though the list of methods declared in the class represented by klass variable, and add those annotated with the specified annotation
            for (final Method method : klass.getDeclaredMethods()) {
                if (method.isAnnotationPresent(Threadable.class)) {
                    methods.add(method);
                }
            }
            // move to the upper class in the hierarchy in search for more methods
            klass = klass.getSuperclass();
        }
        return methods;
    }

    @Override
    public void run() {
        try {
            MDC.put("gridcapa-task-id", getName());
            U threadResult = (U) this.run.invoke(threadable, args);
            this.result = ThreadLauncherResult.success(threadResult);
        } catch (Exception e) {
            if (checkInterruption(e)) {
                //an interruption is not considered as an error because it is intentional
                this.result = ThreadLauncherResult.interrupt();
            } else {
                this.result = ThreadLauncherResult.error(e);
            }
        }
    }

    public ThreadLauncherResult<U> getResult() {
        try {
            join();
        } catch (InterruptedException e) {
            interrupt();
        }
        return result;
    }

    private boolean checkInterruption(Exception exception) {
        boolean isInterrupted = false;
        Throwable e = exception;
        while (e != null && !isInterrupted) {
            if (StringUtils.containsIgnoreCase(e.getMessage(), "interrupted")
                || StringUtils.containsIgnoreCase(e.getClass().getName(), "interrupt")) {
                isInterrupted = true;
            }
            e = e.getCause();
        }
        return isInterrupted;
    }
}
