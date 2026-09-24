package com.devpilot.ai.ai.model;

@FunctionalInterface
public interface AIStreamConsumer {
    void onNext(String token);

    default void onComplete() {}

    default void onError(Throwable throwable) {}
}
