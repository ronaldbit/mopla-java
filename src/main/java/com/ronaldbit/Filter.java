package com.ronaldbit.mopla;

@FunctionalInterface
public interface Filter {
  String apply(Object value, String... args);
}
