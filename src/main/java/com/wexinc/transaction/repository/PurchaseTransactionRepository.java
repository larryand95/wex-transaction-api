package com.wexinc.transaction.repository;

import com.wexinc.transaction.domain.entity.PurchaseTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface PurchaseTransactionRepository extends JpaRepository<PurchaseTransaction, UUID> {
}
