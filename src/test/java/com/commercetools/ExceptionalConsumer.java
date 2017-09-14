package com.commercetools;

@FunctionalInterface
public interface ExceptionalConsumer<T> {
    void accept(final T t) throws Exception;
}