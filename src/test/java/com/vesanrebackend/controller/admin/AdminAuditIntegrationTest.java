package com.vesanrebackend.controller.admin;

import com.vesanrebackend.BookingApiTestSupport;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class AdminAuditIntegrationTest extends BookingApiTestSupport {

    @Test
    void filtersByEntityTypeAndActorNewestFirst() {
        Admin a = admin();
        UUID u = customer().userId();
        assertThat(post(client(), "/api/admin/users/" + u + "/suspend", Map.of("reason", "spam"), a.token())
                .getStatusCode().value()).isEqualTo(200);
        assertThat(post(client(), "/api/admin/users/" + u + "/reactivate", Map.of(), a.token())
                .getStatusCode().value()).isEqualTo(200);
        Map body = get(client(), "/api/admin/audit-logs?entityType=USER&actorId=" + a.userId(), a.token()).getBody();
        List<Map> items = (List<Map>) body.get("items");
        assertThat(items).extracting(i -> i.get("action")).containsExactly("USER_REACTIVATED", "USER_SUSPENDED");
        assertThat(items).extracting(i -> i.get("entityId")).containsOnly(u.toString());
        assertThat(((Map) items.get(1).get("afterData")).get("reason")).isEqualTo("spam");
        assertThat(((Map) items.get(1).get("beforeData")).get("status")).isEqualTo("ACTIVE");
        assertThat(((Map) items.get(0).get("actor")).get("id")).isEqualTo(a.userId().toString());
        assertThat(((Map) items.get(0).get("actor")).get("displayName")).isEqualTo("IT Admin");
        assertThat(get(client(), "/api/admin/audit-logs?entityType=user&actorId=" + a.userId(), a.token())
                .getBody().get("totalElements")).isEqualTo(2);
        assertThat(get(client(), "/api/admin/audit-logs?entityType=VENUE&actorId=" + a.userId(), a.token())
                .getBody().get("totalElements")).isEqualTo(0);
    }

    @Test
    void nonAdminForbidden() {
        Provider p = verifiedProvider();
        assertThat(get(client(), "/api/admin/audit-logs", p.token()).getStatusCode().value()).isEqualTo(403);
    }
}
