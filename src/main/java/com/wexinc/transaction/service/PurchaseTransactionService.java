package com.wexinc.transaction.service;

import com.wexinc.transaction.client.TreasuryExchangeRateClient;
import com.wexinc.transaction.domain.entity.PurchaseTransaction;
import com.wexinc.transaction.domain.model.*;
import com.wexinc.transaction.exception.CurrencyConversionException;
import com.wexinc.transaction.exception.TransactionNotFoundException;
import com.wexinc.transaction.repository.PurchaseTransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PurchaseTransactionService {

    private final PurchaseTransactionRepository repository;
    private final TreasuryExchangeRateClient exchangeRateClient;
    private final TransactionMapper transactionMapper;

    /**
     * Store a new purchase transaction.
     * The purchase amount is rounded to the nearest cent.
     *
     * @param request the transaction details
     * @return the stored transaction with its generated ID
     */
    @Transactional
    public TransactionResponse createTransaction(CreateTransactionRequest request) {
        log.info("Creating purchase transaction: description={}, date={}, amount={}",
                request.getDescription(), request.getTransactionDate(), request.getPurchaseAmount());

        PurchaseTransaction entity = transactionMapper.toEntity(request);
        PurchaseTransaction saved = repository.save(entity);

        log.info("Purchase transaction created with id={}", saved.getId());
        return transactionMapper.toResponse(saved);
    }

    /**
     * Retrieve a purchase transaction converted to the specified country's currency.
     *
     * @param id       the transaction UUID
     * @param country  the country name for currency lookup
     * @param currency the currency name for conversion
     * @return the transaction with converted amount
     * @throws TransactionNotFoundException  if no transaction exists with the given id
     * @throws CurrencyConversionException   if no valid exchange rate is available
     */
    @Transactional(readOnly = true)
    public ConvertedTransactionResponse getTransactionInCurrency(UUID id, String country, String currency) {
        log.info("Retrieving transaction id={} in country={}, currency={}", id, country, currency);

        PurchaseTransaction transaction = repository.findById(id)
                .orElseThrow(() -> new TransactionNotFoundException(id));

        TreasuryExchangeRateResponse.ExchangeRateData rateData =
                exchangeRateClient.getExchangeRate(country, currency, transaction.getTransactionDate())
                        .orElseThrow(() -> new CurrencyConversionException(country, currency));

        BigDecimal convertedAmount = transaction.getPurchaseAmount()
                .multiply(rateData.getExchangeRate())
                .setScale(2, RoundingMode.HALF_UP);

        log.info("Converted transaction id={}: amount={} * rate={} = {}",
                id, transaction.getPurchaseAmount(), rateData.getExchangeRate(), convertedAmount);

        return ConvertedTransactionResponse.builder()
                .id(transaction.getId())
                .description(transaction.getDescription())
                .transactionDate(transaction.getTransactionDate())
                .originalAmount(transaction.getPurchaseAmount())
                .exchangeRate(rateData.getExchangeRate())
                .convertedAmount(convertedAmount)
                .currency(rateData.getCurrency())
                .country(rateData.getCountry())
                .build();
    }
}
