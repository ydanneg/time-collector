package io.ydanneg.service;

import java.time.Instant;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import io.reactivex.Observable;

@Service
public class TimestampService {

    @Value( "${io.ydanneg.timestamp.period:1000}" )
    private Long periodInMillis;

    public Observable<Long> getTimestamps(long period, TimeUnit timeUnit) {
        return Observable.interval(period, timeUnit)
//                .doOnNext(System.out::println)
                .map(ignored -> Instant.now().toEpochMilli());
    }

}
