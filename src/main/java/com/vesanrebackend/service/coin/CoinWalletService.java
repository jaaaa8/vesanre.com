package com.vesanrebackend.service.coin;

import com.vesanrebackend.dto.coin.CoinEntryItem;
import com.vesanrebackend.entity.booking.Booking;
import com.vesanrebackend.entity.coin.CoinLedgerEntry;
import com.vesanrebackend.entity.coin.CoinTopUp;
import com.vesanrebackend.entity.coin.CoinWallet;
import com.vesanrebackend.entity.enums.CoinEntryType;
import com.vesanrebackend.repository.CoinLedgerEntryRepository;
import com.vesanrebackend.repository.CoinWalletRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

/** Coin wallets and the append-only ledger; mutating calls join the caller's transaction. */
@Service
public class CoinWalletService {
    private final CoinWalletRepository wallets;
    private final CoinLedgerEntryRepository ledger;

    public CoinWalletService(CoinWalletRepository wallets, CoinLedgerEntryRepository ledger) {
        this.wallets = wallets;
        this.ledger = ledger;
    }

    @Transactional
    public CoinWallet lockUserWallet(UUID userId) {
        wallets.insertForUser(userId);
        return wallets.findByUserIdForUpdate(userId).orElseThrow();
    }

    @Transactional
    public CoinWallet lockShopWallet(UUID shopId) {
        wallets.insertForShop(shopId);
        return wallets.findByShopIdForUpdate(shopId).orElseThrow();
    }

    /** For callers that only hold the wallet id: never load the wallet entity before this call. */
    @Transactional
    public CoinWallet lockWallet(UUID walletId) {
        return wallets.findByIdForUpdate(walletId).orElseThrow();
    }

    /** The wallet must be locked by the caller; amount is signed. */
    @Transactional
    public CoinLedgerEntry apply(CoinWallet wallet, CoinEntryType type, long amount, Booking booking, CoinTopUp topUp) {
        long after = wallet.getBalance() + amount;
        if (after < 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Insufficient coins");
        }
        wallet.setBalance(after);
        CoinLedgerEntry entry = new CoinLedgerEntry();
        entry.setWallet(wallet);
        entry.setEntryType(type);
        entry.setAmount(amount);
        entry.setBalanceAfter(after);
        entry.setBooking(booking);
        entry.setTopUp(topUp);
        return ledger.saveAndFlush(entry);
    }

    @Transactional(readOnly = true)
    public long userBalance(UUID userId) {
        return wallets.findByUserId(userId).map(CoinWallet::getBalance).orElse(0L);
    }

    @Transactional(readOnly = true)
    public long shopBalance(UUID shopId) {
        return wallets.findByShopId(shopId).map(CoinWallet::getBalance).orElse(0L);
    }

    // Mapped here: booking is LAZY and open-in-view is off.
    @Transactional(readOnly = true)
    public Page<CoinEntryItem> userEntries(UUID userId, Pageable pageable) {
        return wallets.findByUserId(userId)
                .map(w -> ledger.findByWalletId(w.getId(), pageable).map(e -> new CoinEntryItem(e.getId(),
                        e.getEntryType().name(), e.getAmount(), e.getBalanceAfter(),
                        e.getBooking() == null ? null : e.getBooking().getBookingCode(), e.getCreatedAt())))
                .orElseGet(() -> Page.empty(pageable));
    }
}
