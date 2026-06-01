package com.wexinc.transaction.controller;

import com.wexinc.transaction.domain.model.ConvertedTransactionResponse;
import com.wexinc.transaction.domain.model.CreateTransactionRequest;
import com.wexinc.transaction.domain.model.TransactionResponse;
import com.wexinc.transaction.service.PurchaseTransactionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/transactions")
@RequiredArgsConstructor
@Tag(name = "Purchase Transactions", description = "API for managing purchase transactions")
public class PurchaseTransactionController {

    private final PurchaseTransactionService transactionService;

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
            summary = "Store a purchase transaction",
            description = "Accepts and stores a purchase transaction with a description, transaction date, and purchase amount in USD.",
            responses = {
                    @ApiResponse(responseCode = "201", description = "Transaction created successfully"),
                    @ApiResponse(responseCode = "400", description = "Invalid request data",
                            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
            }
    )
    public TransactionResponse createTransaction(
            @Valid @RequestBody CreateTransactionRequest request) {
        return transactionService.createTransaction(request);
    }

    @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
            summary = "Retrieve a purchase transaction in a specified currency",
            description = "Retrieves a stored purchase transaction and converts the amount to the specified country's currency using Treasury exchange rates.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Transaction retrieved and converted successfully"),
                    @ApiResponse(responseCode = "404", description = "Transaction not found",
                            content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
                    @ApiResponse(responseCode = "422", description = "Currency conversion unavailable",
                            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
            }
    )
    public ConvertedTransactionResponse getTransactionInCurrency(
            @Parameter(description = "Transaction unique identifier") @PathVariable UUID id,
            @Parameter(description = "Country name as listed in Treasury Rates of Exchange (e.g., 'Canada')", required = true)
            @RequestParam String country,
            @Parameter(description = "Currency name as listed in Treasury Rates of Exchange (e.g., 'Dollar')", required = true)
            @RequestParam String currency) {
        return transactionService.getTransactionInCurrency(id, country, currency);
    }
}
