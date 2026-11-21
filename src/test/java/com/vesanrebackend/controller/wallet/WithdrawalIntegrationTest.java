package com.vesanrebackend.controller.wallet;

import com.vesanrebackend.BookingApiTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Loại test: integration HTTP thật + PostgreSQL (kế thừa BookingApiTestSupport).
 * API: /api/provider/withdrawals, /api/admin/withdrawals.
 */
class WithdrawalIntegrationTest extends BookingApiTestSupport {
    private ResponseEntity<Map> withdraw(String token, long coins) {
        return withdraw(token, coins, "VCB");
    }

    private ResponseEntity<Map> withdraw(String token, long coins, String bankName) {
        return post(client(), "/api/provider/withdrawals", Map.of("coinAmount", coins, "bankName", bankName,
                "bankAccountNumber", "0123456789", "bankAccountName", "NGUYEN VAN A"), token);
    }

    // Kiểm tra (Review Focus 2): tạo lệnh trừ coin ngay; X < 100 -> 400; số dư < X -> 409; ví = tổng sổ.
    @Test
    void createDeductsImmediately() {
        BookableCourt bc = bookableCourt();
        seedShopCoins(bc, 150);
        String t = bc.provider().token();
        assertThat(withdraw(t, 99).getStatusCode().value()).isEqualTo(400);
        assertThat(withdraw(t, 151).getStatusCode().value()).isEqualTo(409);
        ResponseEntity<Map> ok = withdraw(t, 100);
        assertThat(ok.getStatusCode().value()).isEqualTo(201);
        assertThat(ok.getBody()).containsEntry("feeCoins", 25).containsEntry("status", "PENDING");
        assertThat(new BigDecimal(ok.getBody().get("payoutVnd").toString())).isEqualByComparingTo("75000");
        assertThat(shopBalance(bc.shopId())).isEqualTo(50);
        assertThat(shopLedgerSum(bc.shopId())).isEqualTo(50);
        assertThat(withdraw(t, 100).getStatusCode().value()).isEqualTo(409); // còn 50
    }

    // Kiểm tra: chủ shop chỉ thấy lệnh của shop mình, mới nhất trước.
    @Test
    @SuppressWarnings("unchecked")
    void listShowsOnlyOwnShopNewestFirst() {
        BookableCourt a = bookableCourt();
        BookableCourt b = bookableCourt();
        seedShopCoins(a, 250);
        seedShopCoins(b, 100);
        String first = (String) withdraw(a.provider().token(), 100).getBody().get("id");
        String second = (String) withdraw(a.provider().token(), 100).getBody().get("id");
        String other = (String) withdraw(b.provider().token(), 100).getBody().get("id");
        List<Map<String, Object>> items = (List<Map<String, Object>>) get(client(), "/api/provider/withdrawals",
                a.provider().token()).getBody().get("items");
        assertThat(items).extracting(i -> i.get("id")).containsExactly(second, first).doesNotContain(other);
    }

    // Kiểm tra (Q22): chủ shop bị khóa dùng token cũ -> 403, số dư không đổi.
    @Test
    void suspendedProviderCannotCreate() {
        BookableCourt bc = bookableCourt();
        seedShopCoins(bc, 150);
        jdbc.update("UPDATE sporthub.users SET status = 'SUSPENDED' WHERE id = ?", bc.provider().userId());
        assertThat(withdraw(bc.provider().token(), 100).getStatusCode().value()).isEqualTo(403);
        assertThat(shopBalance(bc.shopId())).isEqualTo(150);
    }

    @Test
    void blankBankFieldsRejected() {
        BookableCourt bc = bookableCourt();
        seedShopCoins(bc, 150);
        assertThat(withdraw(bc.provider().token(), 100, "").getStatusCode().value()).isEqualTo(400);
        assertThat(shopBalance(bc.shopId())).isEqualTo(150);
    }

    @Test
    void customerForbidden() {
        assertThat(withdraw(customer().token(), 100).getStatusCode().value()).isEqualTo(403);
    }
}
