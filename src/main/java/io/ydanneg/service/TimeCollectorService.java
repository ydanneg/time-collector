package io.ydanneg.service;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import io.reactivex.BackpressureOverflowStrategy;
import io.reactivex.Flowable;
import io.reactivex.Scheduler;
import io.reactivex.schedulers.Schedulers;

@Service
@RequiredArgsConstructor
@Slf4j
public class TimeCollectorService {

    private static final int RETRY_DELAY = 500;

    @Autowired
    private final TimestampService timestampService;
    @Autowired
    private final TimestampDaoService timestampDaoService;

    public void startService() {

        final Scheduler single = Schedulers.single();
        Flowable.interval(100, TimeUnit.MILLISECONDS)
                //                .observeOn(Schedulers.newThread())
                .doOnNext(aLong -> log.debug("emitted: " + aLong))
                .onBackpressureBuffer(50, () -> log.info("DROP"), BackpressureOverflowStrategy.DROP_OLDEST)
                .observeOn(single)
                .doOnNext(aLong -> log.debug("pressured: " + aLong))
                .flatMapCompletable(ts -> timestampDaoService.saveOne(ts)
                        .doOnError(error -> log.warn("failed to save into DB."))
                        .retryWhen(throwableFlowable -> throwableFlowable.flatMap(throwable -> {
                            if (throwable instanceof IOException) {
                                log.info("retry scheduled");
                                // schedule retry
                                return Flowable.timer(RETRY_DELAY, TimeUnit.MILLISECONDS, single);
                            }
                            // propagate unexpected error
                            return Flowable.error(throwable);
                        })), false, 1)

                .blockingAwait();

        //
        //
        //        final Flowable<Long> observable = timestampService.getTimestamps(1, TimeUnit.MILLISECONDS)
        //                .onBackpressureBuffer(1, () -> {
        //                    log.info("DROP");
        //                }, BackpressureOverflowStrategy.DROP_OLDEST)
        //                //                .doOnNext(System.out::println)
        //                .doOnError(error -> log.error("failed to get a timestamp", error))
        //                //                .subscribeOn(Schedulers.single())
        //                .share(); // make it connectable for multicasting
        //        //
        //        //        try {
        //        //            Thread.sleep(5000);
        //        //        } catch (InterruptedException e) {
        //        //            e.printStackTrace();
        //        //        }
        //
        //        observable
        //                .subscribeOn(Schedulers.newThread())
        //                .subscribe(aLong -> log.info("sub1: " + aLong), System.err::println);
        //
        ////        final Scheduler single = Schedulers.single();
        ////        observable
        ////                .subscribeOn(Schedulers.computation())
        ////                .window(1, TimeUnit.SECONDS)
        ////                .flatMapSingle(Flowable::toList)
        ////                .observeOn(single)
        ////                .flatMapCompletable(longs -> timestampDaoService.save(longs)
        ////                        .doOnError(error -> log.warn("failed to save into DB.")), false, 1
        ////                )
        ////                .retryWhen(throwableFlowable -> throwableFlowable.flatMap(throwable -> {
        ////                    if (throwable instanceof IOException) {
        ////                        log.info("retry scheduled");
        ////                        // schedule retry
        ////                        return Flowable.timer(RETRY_DELAY, TimeUnit.MILLISECONDS, single);
        ////                    }
        ////                    // propagate unexpected error
        ////                    return Flowable.error(throwable);
        ////                }, false, 1))
        ////                //                .subscribeOn(Schedulers.newThread())
        ////                .doOnError(error -> log.error("error: " + error))
        ////                .subscribe();
        //
        //        observable
        //                .onBackpressureBuffer(1, () -> {
        //                    log.info("DROP");
        //                }, BackpressureOverflowStrategy.DROP_OLDEST)
        //                .subscribeOn(Schedulers.newThread())
        //                .flatMapCompletable(timestampDaoService::saveOne)
        //                .blockingGet();
        ////                .blockingSubscribe(); // block until complete or error
    }

    private Flowable<Long> retryOnIOException(Flowable<Long> completable) {
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
