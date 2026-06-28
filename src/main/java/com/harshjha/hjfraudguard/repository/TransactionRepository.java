package com.harshjha.hjfraudguard.repository;

import com.harshjha.hjfraudguard.model.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {
}