package io.ydanneg.service;

import io.reactivex.Observable;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.concurrent.TimeUnit;

@Service
public class TimestampService {

    public Observable<Long> getTimestamps(long period, TimeUnit timeUnit) {
        return Observable.interval(period, timeUnit)
                .map(ignored -> Instant.now().toEpochMilli());
    }
}
