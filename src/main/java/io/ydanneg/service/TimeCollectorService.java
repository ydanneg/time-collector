package io.ydanneg.service;

import io.reactivex.processors.PublishProcessor;
import io.reactivex.schedulers.Schedulers;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;


@Service
@RequiredArgsConstructor
@Slf4j
public class TimeCollectorService {

    @Autowired
    private final TimestampService timestampService;

    public void startService() {

//        PublishProcessor<Integer> source = PublishProcessor.create();
//
//        source
//                .sample(1, TimeUnit.MILLISECONDS)
//                .observeOn(Schedulers.computation(), true, 1024)
//                .subscribe(v -> {}, Throwable::printStackTrace);
//
//        for (int i = 0; i < 1_000_000; i++) {
//            source.onNext(i);
//        }

        timestampService.getTimestamps(1, TimeUnit.SECONDS)
                .doOnNext(System.out::println)
                .doOnError(error -> log.error("failed to get a timestamp", error))

                .blockingSubscribe(); // block until complete or error
    }
}
