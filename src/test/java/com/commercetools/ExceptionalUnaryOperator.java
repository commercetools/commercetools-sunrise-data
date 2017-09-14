package com.commercetools;

@FunctionalInterface
public interface ExceptionalUnaryOperator<T> {
    T apply(final T t) throws Exception;
}