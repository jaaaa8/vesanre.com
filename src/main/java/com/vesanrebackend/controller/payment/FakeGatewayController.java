package com.vesanrebackend.controller.payment;

import com.vesanrebackend.entity.coin.CoinTopUp;
import com.vesanrebackend.entity.enums.CoinTopUpStatus;
import com.vesanrebackend.repository.CoinTopUpRepository;
import com.vesanrebackend.service.coin.TopUpService;
import com.vesanrebackend.service.payment.FakeTopUpGateway;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.util.HtmlUtils;

import java.net.URI;
import java.nio.charset.StandardCharsets;

/** Dev-only payment page: the confirm button signs a payload and runs the same path as a real webhook. */
@Controller
@ConditionalOnProperty(name = "app.payment.fake.enabled", havingValue = "true")
@RequestMapping("/api/payment/fake")
public class FakeGatewayController {
    private static final MediaType HTML = new MediaType("text", "html", StandardCharsets.UTF_8);

    private final CoinTopUpRepository topUps;
    private final TopUpService topUpService;
    private final FakeTopUpGateway gateway;

    public FakeGatewayController(CoinTopUpRepository topUps, TopUpService topUpService, FakeTopUpGateway gateway) {
        this.topUps = topUps;
        this.topUpService = topUpService;
        this.gateway = gateway;
    }

    @GetMapping("/{transferCode}")
    public ResponseEntity<String> page(@PathVariable String transferCode) {
        return topUps.findByTransferCode(transferCode)
                .map(t -> html(HttpStatus.OK, render(t)))
                .orElseGet(this::notFound);
    }

    @PostMapping("/{transferCode}/confirm")
    public ResponseEntity<String> confirm(@PathVariable String transferCode) {
        CoinTopUp t = topUps.findByTransferCode(transferCode).orElse(null);
        if (t == null) {
            return notFound();
        }
        FakeTopUpGateway.SignedEvent e = gateway.signedPaidEvent(t);
        topUpService.receive(gateway.code(), e.body().getBytes(StandardCharsets.UTF_8), e.headers());
        return ResponseEntity.status(HttpStatus.SEE_OTHER).location(URI.create("/api/payment/fake/" + t.getTransferCode()))
                .build();
    }

    private ResponseEntity<String> notFound() {
        return html(HttpStatus.NOT_FOUND, shell("<p>Không tìm thấy lệnh nạp</p>"));
    }

    private static ResponseEntity<String> html(HttpStatus status, String body) {
        return ResponseEntity.status(status).contentType(HTML).body(body);
    }

    private static String render(CoinTopUp t) {
        String code = HtmlUtils.htmlEscape(t.getTransferCode());
        String vnd = String.format("%,d", t.getCoinAmount() * 1000).replace(',', '.');
        String status = switch (t.getStatus()) {
            case PENDING -> "Chờ thanh toán";
            case PAID -> "Đã thanh toán";
            case EXPIRED -> "Đã hết hạn";
        };
        String form = t.getStatus() == CoinTopUpStatus.PAID ? ""
                : "<form method=\"post\" action=\"/api/payment/fake/" + code + "/confirm\">"
                + "<button type=\"submit\">Xác nhận đã chuyển</button></form>";
        return shell("<p>Nội dung: <b>" + code + "</b></p><p>Số tiền: <b>" + vnd + "đ</b></p>"
                + "<p>Trạng thái: <b>" + status + "</b></p>" + form);
    }

    private static String shell(String content) {
        return "<!doctype html><html lang=\"vi\"><head><meta charset=\"utf-8\">"
                + "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">"
                + "<title>Cổng thanh toán giả lập</title></head>"
                + "<body style=\"font-family:sans-serif;max-width:360px;margin:2rem auto;padding:0 1rem\">"
                + "<h2>Cổng thanh toán giả lập</h2>" + content + "</body></html>";
    }
}
