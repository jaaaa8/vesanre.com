package com.vesanrebackend.controller.payment;

import com.vesanrebackend.service.coin.TopUpService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/payment/webhooks")
public class PaymentWebhookController {
    private final TopUpService topUps;

    public PaymentWebhookController(TopUpService topUps) {
        this.topUps = topUps;
    }

    // Raw body: the signature covers the exact bytes.
    @PostMapping("/{provider}")
    public ResponseEntity<Void> receive(@PathVariable String provider, @RequestBody byte[] body,
                                        @RequestHeader HttpHeaders headers) {
        topUps.receive(provider, body, headers);
        return ResponseEntity.ok().build();
    }
}
