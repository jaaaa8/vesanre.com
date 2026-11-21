package com.vesanrebackend.service.coin;

import com.vesanrebackend.dto.admin.PageResponse;
import com.vesanrebackend.dto.coin.CreateWithdrawalRequest;
import com.vesanrebackend.dto.coin.WithdrawalView;
import com.vesanrebackend.entity.account.UserAccount;
import com.vesanrebackend.entity.coin.CoinWallet;
import com.vesanrebackend.entity.coin.ShopWithdrawal;
import com.vesanrebackend.entity.enums.CoinEntryType;
import com.vesanrebackend.entity.enums.UserStatus;
import com.vesanrebackend.entity.shop.Shop;
import com.vesanrebackend.repository.ShopRepository;
import com.vesanrebackend.repository.ShopWithdrawalRepository;
import com.vesanrebackend.repository.UserAccountRepository;
import com.vesanrebackend.util.Paging;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.UUID;

/** Shop coin withdrawals. Lock order is always withdrawal row -> shop wallet. */
@Service
public class WithdrawalService {
    private final ShopWithdrawalRepository withdrawals;
    private final ShopRepository shops;
    private final UserAccountRepository users;
    private final CoinWalletService coinWallets;

    public WithdrawalService(ShopWithdrawalRepository withdrawals, ShopRepository shops, UserAccountRepository users,
                             CoinWalletService coinWallets) {
        this.withdrawals = withdrawals;
        this.shops = shops;
        this.users = users;
        this.coinWallets = coinWallets;
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

    @Transactional(readOnly = true)
    public PageResponse<WithdrawalView> list(UUID ownerUserId, int page, int size) {
        Shop shop = shops.findByOwnerUserId(ownerUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Shop not found"));
        return PageResponse.of(withdrawals.findByShopId(shop.getId(),
                Paging.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt", "id"))).map(WithdrawalView::of));
    }
}
