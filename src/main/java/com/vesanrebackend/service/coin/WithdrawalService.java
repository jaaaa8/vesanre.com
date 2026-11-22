package com.vesanrebackend.service.coin;

import com.vesanrebackend.dto.admin.PageResponse;
import com.vesanrebackend.dto.coin.CreateWithdrawalRequest;
import com.vesanrebackend.dto.coin.WithdrawalView;
import com.vesanrebackend.entity.account.UserAccount;
import com.vesanrebackend.entity.coin.CoinWallet;
import com.vesanrebackend.entity.coin.ShopWithdrawal;
import com.vesanrebackend.entity.enums.CoinEntryType;
import com.vesanrebackend.entity.enums.UserStatus;
import com.vesanrebackend.entity.enums.WithdrawalStatus;
import com.vesanrebackend.entity.shop.Shop;
import com.vesanrebackend.repository.NotificationRepository;
import com.vesanrebackend.repository.ShopRepository;
import com.vesanrebackend.repository.ShopWithdrawalRepository;
import com.vesanrebackend.repository.UserAccountRepository;
import com.vesanrebackend.service.admin.AdminAudit;
import com.vesanrebackend.service.mail.ReviewMailer;
import com.vesanrebackend.util.Paging;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/** Shop coin withdrawals. Lock order is always withdrawal row -> shop wallet. */
@Service
public class WithdrawalService {
    private static final String ENTITY_TYPE = "WITHDRAWAL";

    private final ShopWithdrawalRepository withdrawals;
    private final ShopRepository shops;
    private final UserAccountRepository users;
    private final CoinWalletService coinWallets;
    private final AdminAudit audit;
    private final NotificationRepository notifications;
    private final ReviewMailer mailer;
    private final Clock clock;

    public WithdrawalService(ShopWithdrawalRepository withdrawals, ShopRepository shops, UserAccountRepository users,
                             CoinWalletService coinWallets, AdminAudit audit, NotificationRepository notifications,
                             ReviewMailer mailer, Clock clock) {
        this.withdrawals = withdrawals;
        this.shops = shops;
        this.users = users;
        this.coinWallets = coinWallets;
        this.audit = audit;
        this.notifications = notifications;
        this.mailer = mailer;
        this.clock = clock;
    }

    /** 20 coins + 5% rounded up. */
    public static long feeCoins(long coinAmount) {
        return 20 + (coinAmount * 5 + 99) / 100;
    }

    @Transactional
    public WithdrawalView create(UUID ownerUserId, CreateWithdrawalRequest req) {
        Shop shop = shops.findByOwnerUserId(ownerUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Shop not found"));
        UserAccount owner = users.findById(ownerUserId).orElseThrow();
        if (owner.getStatus() != UserStatus.ACTIVE) { // the token may predate a suspension
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Account is not active");
        }
        long coins = req.coinAmount();
        long fee = feeCoins(coins);
        CoinWallet wallet = coinWallets.lockShopWallet(shop.getId());
        if (wallet.getBalance() < coins) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Insufficient coins");
        }
        ShopWithdrawal w = new ShopWithdrawal();
        w.setShop(shop);
        w.setRequestedBy(owner);
        w.setCoinAmount(coins);
        w.setFeeCoins(fee);
        w.setPayoutVnd(BigDecimal.valueOf((coins - fee) * 1000));
        w.setBankName(req.bankName().strip());
        w.setBankAccountNumber(req.bankAccountNumber().strip());
        w.setBankAccountName(req.bankAccountName().strip());
        w = withdrawals.saveAndFlush(w);
        coinWallets.apply(wallet, CoinEntryType.WITHDRAWAL, -coins, w);
        return WithdrawalView.of(w);
    }

    // Oldest first so the longest-waiting request tops the admin queue; null status = all.
    @Transactional(readOnly = true)
    public PageResponse<WithdrawalView> adminList(WithdrawalStatus status, int page, int size) {
        Pageable pageable = Paging.of(page, size, "id");
        return PageResponse.of((status == null ? withdrawals.findAllWithShop(pageable)
                : withdrawals.findByStatus(status, pageable)).map(WithdrawalView::of));
    }

    @Transactional
    public WithdrawalView markPaid(UUID adminId, UUID id, String ref) {
        ShopWithdrawal w = lockPending(id);
        Instant now = clock.instant();
        w.setStatus(WithdrawalStatus.PAID);
        w.setBankTransactionRef(ref.strip());
        w.setReviewedBy(users.getReferenceById(adminId));
        w.setReviewedAt(now);
        w.setPaidAt(now);
        Map<String, Object> after = new LinkedHashMap<>();
        after.put("status", "PAID");
        after.put("bankTransactionRef", w.getBankTransactionRef());
        audit.record(adminId, "WITHDRAWAL_PAID", ENTITY_TYPE, id, Map.of("status", "PENDING"), after);
        notifyOwner(w, "Đã chuyển tiền rút coin", "Lệnh rút " + w.getCoinAmount() + " coin đã chuyển "
                + vnd(w.getPayoutVnd()) + "đ, mã GD " + w.getBankTransactionRef(), null);
        return WithdrawalView.of(w);
    }

    @Transactional
    public WithdrawalView reject(UUID adminId, UUID id, String reason) {
        ShopWithdrawal w = lockPending(id);
        w.setStatus(WithdrawalStatus.REJECTED);
        w.setRejectionReason(reason.strip());
        w.setReviewedBy(users.getReferenceById(adminId));
        w.setReviewedAt(clock.instant());
        CoinWallet wallet = coinWallets.lockShopWallet(w.getShop().getId());
        coinWallets.apply(wallet, CoinEntryType.WITHDRAWAL_RETURN, w.getCoinAmount(), w);
        Map<String, Object> after = new LinkedHashMap<>();
        after.put("status", "REJECTED");
        after.put("reason", w.getRejectionReason());
        audit.record(adminId, "WITHDRAWAL_REJECTED", ENTITY_TYPE, id, Map.of("status", "PENDING"), after);
        notifyOwner(w, "Lệnh rút coin bị từ chối", "Đã hoàn " + w.getCoinAmount() + " coin vào ví shop",
                w.getRejectionReason());
        return WithdrawalView.of(w);
    }

    // Row lock first: a second admin waits, then sees the new status and gets 409.
    private ShopWithdrawal lockPending(UUID id) {
        ShopWithdrawal w = withdrawals.findByIdForUpdate(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Withdrawal not found"));
        if (w.getStatus() != WithdrawalStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Withdrawal is not PENDING");
        }
        return w;
    }

    private void notifyOwner(ShopWithdrawal w, String title, String body, String reason) {
        var owner = w.getShop().getOwner();
        String type = "WITHDRAWAL_" + w.getStatus().name();
        notifications.insertIfAbsent(owner.getUserId(), null, type, title,
                reason == null ? body : body + ". Lý do: " + reason, "withdrawal:" + w.getId() + ":" + w.getStatus().name());
        mailer.send(owner.getUser().getEmail(), "[Vesanre] " + title, body, reason);
    }

    private static String vnd(BigDecimal amount) {
        return String.format(Locale.ROOT, "%,d", amount.longValue()).replace(',', '.');
    }

    @Transactional(readOnly = true)
    public PageResponse<WithdrawalView> list(UUID ownerUserId, int page, int size) {
        Shop shop = shops.findByOwnerUserId(ownerUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Shop not found"));
        return PageResponse.of(withdrawals.findByShopId(shop.getId(),
                Paging.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt", "id"))).map(WithdrawalView::of));
    }
}
