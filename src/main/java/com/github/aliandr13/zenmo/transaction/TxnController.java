package com.github.aliandr13.zenmo.transaction;

import com.github.aliandr13.zenmo.transaction.dto.TxnRequest;
import com.github.aliandr13.zenmo.transaction.dto.TxnResponse;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/transactions")
public class TxnController {

    private final TxnService txnService;

    public TxnController(TxnService txnService) {
        this.txnService = txnService;
    }

    @GetMapping
    public ResponseEntity<Page<TxnResponse>> list(
            @RequestParam(required = false) UUID accountId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            Pageable pageable
    ) {
        return ResponseEntity.ok(txnService.list(
                Optional.ofNullable(accountId),
                Optional.ofNullable(fromDate),
                Optional.ofNullable(toDate),
                pageable
        ));
    }

    @GetMapping("/{id}")
    public ResponseEntity<TxnResponse> get(@PathVariable UUID id) {
        return ResponseEntity.ok(txnService.get(id));
    }

    @PostMapping
    public ResponseEntity<TxnResponse> create(@Valid @RequestBody TxnRequest request) {
        return ResponseEntity.ok(txnService.create(request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        txnService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
