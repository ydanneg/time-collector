package io.ydanneg.service;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import io.reactivex.BackpressureStrategy;
import io.reactivex.Completable;
import io.reactivex.Flowable;
import io.reactivex.Observable;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.PublishSubject;

@Service
@RequiredArgsConstructor
@Slf4j
public class TimeCollectorService {

    private static final int RETRY_DELAY = 5000;

    @Autowired
    private final TimestampService timestampService;
    @Autowired
    private final TimestampDaoService timestampDaoService;

    PublishSubject<Long> source = PublishSubject.create();

    public void startService() {

        final Observable<Long> observable = timestampService.getTimestamps(1, TimeUnit.SECONDS)
                .doOnError(error -> log.error("failed to get a timestamp", error))
                .share(); // make it connectable for multicasting

        observable
                .observeOn(Schedulers.newThread())
                .subscribe(aLong -> log.info("sub1: " + aLong), System.err::println);

        observable
                .toFlowable(BackpressureStrategy.BUFFER)
                .buffer(5000, TimeUnit.MILLISECONDS)
                //                .window(5)
                //                .flatMapSingle(Flowable::toList)
                .doOnNext(ts -> log.trace("sub2: " + ts))
                .flatMapCompletable(longs -> timestampDaoService.save(longs)
                        .doOnError(error -> log.warn("failed to save into DB."))
                )
                .compose(this::retryOnIOException)
                //                .observeOn(Schedulers.computation())
                .subscribe(() -> log.trace("sub2.complete"), error -> log.error("", error));

        observable
                .blockingSubscribe(); // block until complete or error
    }

    private Completable retryOnIOException(Completable completable) {
        return completable.retryWhen(throwableFlowable -> throwableFlowable.flatMap(throwable -> {
            if (throwable instanceof IOException) {
                log.info("retry scheduled");
                // schedule retry
                return Flowable.timer(RETRY_DELAY, TimeUnit.MILLISECONDS);
            }
            // propagate unexpected error
            return Flowable.error(throwable);
        }));
    }
}
