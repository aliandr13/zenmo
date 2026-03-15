package com.github.aliandr13.zenmo.transaction;

import com.github.aliandr13.zenmo.transaction.dto.TxnRequest;
import com.github.aliandr13.zenmo.transaction.dto.TxnResponse;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for transaction endpoints.
 */
@RestController
@RequestMapping("/api/transactions")
public class TxnController {

    private final TxnService txnService;

    /**
     * Constructor.
     */
    public TxnController(TxnService txnService) {
        this.txnService = txnService;
    }

    /**
     * Returns a page of transactions, optionally filtered.
     */
    @GetMapping
    public ResponseEntity<Page<TxnResponse>> list(
            @RequestParam(required = false) String accountId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate toDate,
            Pageable pageable
    ) {
        return ResponseEntity.ok(txnService.list(
                Optional.ofNullable(accountId).map(UUID::fromString),
                Optional.ofNullable(fromDate),
                Optional.ofNullable(toDate),
                pageable
        ));
    }

    /**
     * Returns a single transaction by id.
     */
    @GetMapping("/{id}")
    public ResponseEntity<TxnResponse> get(@PathVariable UUID id) {
        return ResponseEntity.ok(txnService.get(id));
    }

    /**
     * Creates a new transaction.
     */
    @PostMapping
    public ResponseEntity<TxnResponse> create(@Valid @RequestBody TxnRequest request) {
        return ResponseEntity.ok(txnService.create(request));
    }

    /**
     * Deletes a transaction by id.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        txnService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
