package com.learning.observability.cashflowservicea.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;

@Service
public class TransactionService {

    private static final Logger logger = LoggerFactory.getLogger(TransactionService.class);

    @Autowired
    private MeterRegistry meterRegistry;

    @Autowired
    private Tracer tracer;

    public String processTransaction(double amount, String transactionName, String company) {
        Span newSpan = tracer.nextSpan().name("processTransaction").start();
        try (Tracer.SpanInScope spanInScope = tracer.withSpan(newSpan.start())) {
            logger.debug("Processing transaction: amount={}, transactionName={}, company={}", amount, transactionName, company);

            Timer.Sample sample = Timer.start(meterRegistry);

            meterRegistry.counter("transactions.attempted", "transactionName", transactionName, "company", company).increment();

            String result;
            if (amount > 0 && transactionName != null && !transactionName.isEmpty() && company != null && !company.isEmpty()) {
                logger.info("Transaction processed successfully: transactionName={}, company={}", transactionName, company);
                result = "Success";
            } else {
                logger.warn("Transaction processing failed: transactionName={}, company={}", transactionName, company);
                result = "Failure";
            }

            sample.stop(meterRegistry.timer("transactions.processing.time", "transactionName", transactionName, "company", company));

            return result;
        } finally {
            newSpan.end();
        }
    }
}