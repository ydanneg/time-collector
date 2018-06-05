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
import io.reactivex.Scheduler;
import io.reactivex.schedulers.Schedulers;

@Service
@RequiredArgsConstructor
@Slf4j
public class TimeCollectorService {

    @Value("${io.ydanneg.timecollector.db.retry.delay:5000}")
    private Long retryDelayInMillis;

    @Value("${io.ydanneg.timecollector.timestamp.period:1000}")
    private Long timestampPeriodInMillis;

    @Value("${io.ydanneg.timecollector.buffer:5000}")
    private Long backpressureBufferLimit;

    @Value("${io.ydanneg.timecollector.window.size:5}")
    private Long windowSize;

    @Autowired
    private final TimestampService timestampService;
    @Autowired
    private final TimestampDaoService timestampDaoService;

    public void startService() {

        final Scheduler single = Schedulers.newThread();
        Flowable.interval(timestampPeriodInMillis, TimeUnit.MILLISECONDS)
                .doOnNext(aLong -> log.debug("emitted: " + aLong))
                .onBackpressureBuffer(backpressureBufferLimit, () -> log.trace("backpressure overflow. dropping oldest"), BackpressureOverflowStrategy.DROP_OLDEST)
                .observeOn(single)
                .window(windowSize)
                .flatMapSingle(Flowable::toList)
                .doOnNext(aLong -> log.debug("processing: " + aLong))
                .flatMapCompletable(ts -> timestampDaoService.saveAll(ts)
                        .doOnError(error -> log.warn("failed to save into DB."))
                        .retryWhen(throwableFlowable -> throwableFlowable.flatMap(throwable -> {
                            if (throwable instanceof IOException) {
                                log.info("retry scheduled");
                                // schedule retry
                                return Flowable.timer(retryDelayInMillis, TimeUnit.MILLISECONDS, single);
                            }
                            // propagate unexpected error
                            return Flowable.error(throwable);
                        })), false, 1)

                .blockingAwait();
    }
}
