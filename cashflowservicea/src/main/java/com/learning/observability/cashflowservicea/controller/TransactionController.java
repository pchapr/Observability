package com.learning.observability.cashflowservicea.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.learning.observability.cashflowservicea.service.TransactionService;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;

@RestController
@RequestMapping("/api/transactions")
public class TransactionController {

    private static final Logger logger = LoggerFactory.getLogger(TransactionController.class);

    @Autowired
    private TransactionService transactionService;

    @Autowired
    private MeterRegistry meterRegistry;

    @Autowired
    private Tracer tracer;

    @PostMapping("/process")
    public String processTransaction(@RequestParam double amount, 
                                      @RequestParam String transactionName, 
                                      @RequestParam String company) {
        Span newSpan = tracer.nextSpan().name("processTransactionController").start();
        try (Tracer.SpanInScope spanInScope = tracer.withSpan(newSpan.start())) {
            logger.info("Received transaction request: amount={}, transactionName={}, company={}", amount, transactionName, company);

            Timer.Sample sample = Timer.start(meterRegistry);

            meterRegistry.counter("transactions.processed", "transactionName", transactionName, "company", company).increment();

            String result = transactionService.processTransaction(amount, transactionName, company);

            sample.stop(meterRegistry.timer("transactions.processing.time", "transactionName", transactionName, "company", company));

            if ("Success".equals(result)) {
                logger.info("Transaction processed successfully: transactionName={}, company={}", transactionName, company);
            } else {
                logger.error("Transaction processing failed: transactionName={}, company={}", transactionName, company);
            }

            return result;
        } finally {
            newSpan.end();
        }
    }
}