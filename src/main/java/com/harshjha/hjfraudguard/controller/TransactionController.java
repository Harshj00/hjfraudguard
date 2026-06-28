package com.harshjha.hjfraudguard.controller;

import com.harshjha.hjfraudguard.model.Transaction;
import com.harshjha.hjfraudguard.repository.TransactionRepository;
import com.harshjha.hjfraudguard.service.FraudDetectionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/transactions")
public class TransactionController {

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private FraudDetectionService fraudDetectionService;

    // CREATE a new transaction (now scored by the ML model before saving)
    @PostMapping
    public ResponseEntity<Transaction> createTransaction(@RequestBody Transaction transaction) throws Exception {
        double anomalyScore = fraudDetectionService.computeAnomalyScore(transaction.getAmount());
        transaction.setAnomalyScore(anomalyScore);
        transaction.setFlaggedFraud(fraudDetectionService.isFraud(anomalyScore));

        Transaction saved = transactionRepository.save(transaction);
        return ResponseEntity.ok(saved);
    }

    // GET all transactions
    @GetMapping
    public ResponseEntity<List<Transaction>> getAllTransactions() {
        List<Transaction> transactions = transactionRepository.findAll();
        return ResponseEntity.ok(transactions);
    }

    // GET one transaction by ID
    @GetMapping("/{id}")
    public ResponseEntity<Transaction> getTransactionById(@PathVariable Long id) {
        return transactionRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}