package io.ydanneg.service;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import io.reactivex.BackpressureOverflowStrategy;
import io.reactivex.Flowable;
import io.reactivex.schedulers.Schedulers;

@Service
@RequiredArgsConstructor
@Slf4j
public class TimeCollectorService {

    @Value("${io.ydanneg.timecollector.db.retry.delay:5000}")
    private Long retryDelayInMillis;

    @Value("${io.ydanneg.timecollector.timestamp.period:1000}")
    private Long timestampPeriodInMillis;

    @Value("${io.ydanneg.timecollector.capacity:5000}")
    private Long capacity;

    @Value("${io.ydanneg.timecollector.window:5}")
    private int windowSize;

    @Autowired
    private final TimestampService timestampService;
    @Autowired
    private final TimestampDaoService timestampDaoService;

    public void startService() {

        final Flowable<Long> intervalPublisher = timestampService.getTimestamps(timestampPeriodInMillis);

        intervalPublisher
                .observeOn(Schedulers.newThread())
                .subscribe(aLong -> log.info(String.valueOf(aLong)));

        intervalPublisher
                .doOnNext(aLong -> log.debug("emitted: " + aLong))
                .onBackpressureBuffer(capacity, () -> log.warn("backpressure buffer overflow. dropping oldest"), BackpressureOverflowStrategy.DROP_OLDEST)
                .observeOn(Schedulers.newThread())
                .doOnNext(aLong -> log.debug("processing: " + aLong))
                .buffer(windowSize)
                .doOnNext(aLong -> log.debug("processing window: " + aLong))
                .flatMapCompletable(longs -> timestampDaoService.saveAll(longs)
                        .doOnError(error -> log.warn("failed to save into DB."))
                        .retryWhen(throwableFlowable -> throwableFlowable.flatMap(throwable -> {
                            if (throwable instanceof IOException) {
                                log.info("retry scheduled");
                                // schedule retry
                                return Flowable.timer(retryDelayInMillis, TimeUnit.MILLISECONDS);
                            }
                            // propagate unexpected error
                            return Flowable.error(throwable);
                        })), false, 1)
                .blockingAwait();
    }
}
