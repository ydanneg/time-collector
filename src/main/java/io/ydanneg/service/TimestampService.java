package io.ydanneg.service;

import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import io.reactivex.Flowable;

@Service
public class TimestampService {

    @Value( "${io.ydanneg.timestamp.period:1000}" )
    private Long periodInMillis;

    public Flowable<Long> getTimestamps(long period, TimeUnit timeUnit) {
        return Flowable.interval(period, timeUnit)
//                .doOnNext(System.out::println)
                .map(ignored -> ignored/*Instant.now().toEpochMilli()*/);
    }

}
