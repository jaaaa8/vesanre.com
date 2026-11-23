package com.vesanrebackend.controller.wallet;

import com.vesanrebackend.BookingApiTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

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
        BookableVenue bc = bookableVenue(1, 30);
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
        BookableVenue a = bookableVenue(1, 30);
        BookableVenue b = bookableVenue(1, 30);
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
        BookableVenue bc = bookableVenue(1, 30);
        seedShopCoins(bc, 150);
        jdbc.update("UPDATE sporthub.users SET status = 'SUSPENDED' WHERE id = ?", bc.provider().userId());
        assertThat(withdraw(bc.provider().token(), 100).getStatusCode().value()).isEqualTo(403);
        assertThat(shopBalance(bc.shopId())).isEqualTo(150);
    }

    @Test
    void blankBankFieldsRejected() {
        BookableVenue bc = bookableVenue(1, 30);
        seedShopCoins(bc, 150);
        assertThat(withdraw(bc.provider().token(), 100, "").getStatusCode().value()).isEqualTo(400);
        assertThat(shopBalance(bc.shopId())).isEqualTo(150);
    }

    @Test
    void customerForbidden() {
        assertThat(withdraw(customer().token(), 100).getStatusCode().value()).isEqualTo(403);
    }

    private int auditCount(String action, String id) {
        return jdbc.queryForObject("SELECT count(*) FROM sporthub.audit_logs WHERE action = ? AND entity_type = 'WITHDRAWAL' "
                + "AND entity_id = ?::uuid", Integer.class, action, id);
    }

    private int notificationCount(UUID userId, String type) {
        return jdbc.queryForObject("SELECT count(*) FROM sporthub.notifications WHERE user_id = ? AND notification_type = ? "
                + "AND booking_id IS NULL", Integer.class, userId, type);
    }

    // Kiểm tra (Review Focus 1, 2): reject hoàn coin đúng một lần; reject/mark-paid lần sau -> 409; ví = tổng sổ.
    @Test
    void rejectRefundsOnceThenMarkPaidConflicts() {
        BookableVenue bc = bookableVenue(1, 30);
        seedShopCoins(bc, 100);
        Admin a = admin();
        String id = (String) withdraw(bc.provider().token(), 100).getBody().get("id");
        assertThat(shopBalance(bc.shopId())).isEqualTo(0);
        ResponseEntity<Map> r = post(client(), "/api/admin/withdrawals/" + id + "/reject", Map.of("reason", "sai STK"),
                a.token());
        assertThat(r.getStatusCode().value()).isEqualTo(200);
        assertThat(r.getBody()).containsEntry("status", "REJECTED").containsEntry("rejectionReason", "sai STK");
        assertThat(shopBalance(bc.shopId())).isEqualTo(100);
        assertThat(post(client(), "/api/admin/withdrawals/" + id + "/reject", Map.of("reason", "x"), a.token())
                .getStatusCode().value()).isEqualTo(409);
        assertThat(post(client(), "/api/admin/withdrawals/" + id + "/mark-paid", Map.of("bankTransactionRef", "FT1"),
                a.token()).getStatusCode().value()).isEqualTo(409);
        assertThat(shopBalance(bc.shopId())).isEqualTo(100);
        assertThat(shopLedgerSum(bc.shopId())).isEqualTo(100);
        assertThat(auditCount("WITHDRAWAL_REJECTED", id)).isEqualTo(1);
        assertThat(notificationCount(bc.provider().userId(), "WITHDRAWAL_REJECTED")).isEqualTo(1);
    }

    // Kiểm tra: mark-paid giữ coin đã trừ, ghi ref/paidAt, audit + thông báo; thiếu ref -> 400.
    @Test
    void markPaidKeepsCoinsDeducted() {
        BookableVenue bc = bookableVenue(1, 30);
        seedShopCoins(bc, 200);
        Admin a = admin();
        String other = (String) withdraw(bc.provider().token(), 100).getBody().get("id");
        assertThat(post(client(), "/api/admin/withdrawals/" + other + "/mark-paid", Map.of("bankTransactionRef", " "),
                a.token()).getStatusCode().value()).isEqualTo(400);
        String id = (String) withdraw(bc.provider().token(), 100).getBody().get("id");
        ResponseEntity<Map> r = post(client(), "/api/admin/withdrawals/" + id + "/mark-paid",
                Map.of("bankTransactionRef", "FT123"), a.token());
        assertThat(r.getStatusCode().value()).isEqualTo(200);
        assertThat(r.getBody()).containsEntry("status", "PAID").containsEntry("bankTransactionRef", "FT123");
        assertThat(r.getBody().get("paidAt")).isNotNull();
        assertThat(shopBalance(bc.shopId())).isEqualTo(0);
        assertThat(shopLedgerSum(bc.shopId())).isEqualTo(0);
        assertThat(auditCount("WITHDRAWAL_PAID", id)).isEqualTo(1);
        assertThat(notificationCount(bc.provider().userId(), "WITHDRAWAL_PAID")).isEqualTo(1);
    }

    // Kiểm tra: lọc PENDING trả lệnh vừa tạo kèm shopName.
    @Test
    void adminListFiltersPending() {
        BookableVenue bc = bookableVenue(1, 30);
        seedShopCoins(bc, 100);
        String id = (String) withdraw(bc.provider().token(), 100).getBody().get("id");
        String shopName = jdbc.queryForObject("SELECT name FROM sporthub.shops WHERE id = ?", String.class, bc.shopId());
        List<Map<String, Object>> items = allItems(client(), "/api/admin/withdrawals?status=PENDING", admin().token());
        assertThat(items).filteredOn(i -> id.equals(i.get("id"))).singleElement()
                .satisfies(i -> assertThat(i).containsEntry("shopName", shopName).containsEntry("status", "PENDING"));
        assertThat(items).allSatisfy(i -> assertThat(i).containsEntry("status", "PENDING"));
    }

    // Kiểm tra (Q22): chủ shop bị khóa sau khi tạo lệnh, admin vẫn duyệt được.
    @Test
    void suspendedOwnerRequestStillReviewable() {
        BookableVenue bc = bookableVenue(1, 30);
        seedShopCoins(bc, 100);
        String id = (String) withdraw(bc.provider().token(), 100).getBody().get("id");
        jdbc.update("UPDATE sporthub.users SET status = 'SUSPENDED' WHERE id = ?", bc.provider().userId());
        assertThat(post(client(), "/api/admin/withdrawals/" + id + "/mark-paid", Map.of("bankTransactionRef", "FT9"),
                admin().token()).getStatusCode().value()).isEqualTo(200);
    }

    @Test
    void providerForbiddenOnAdminApi() {
        assertThat(get(client(), "/api/admin/withdrawals", bookableVenue(1, 30).provider().token())
                .getStatusCode().value()).isEqualTo(403);
    }
}
