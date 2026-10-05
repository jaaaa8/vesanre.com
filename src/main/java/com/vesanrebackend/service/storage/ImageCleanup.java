package com.vesanrebackend.service.storage;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.List;

// Storage objects follow the DB: dropped only after the referencing rows are gone (commit),
// and a fresh upload is dropped when its transaction rolls back. Publish inside the transaction.
@Component
public class ImageCleanup {
    public record ImagesDeletedEvent(List<String> publicIds) {
    }

    public record ImageUploadedEvent(String publicId) {
    }

    private final ImageStorage storage;

    public ImageCleanup(ImageStorage storage) {
        this.storage = storage;
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void deleted(ImagesDeletedEvent event) {
        event.publicIds().forEach(storage::delete);
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_ROLLBACK)
    public void rolledBack(ImageUploadedEvent event) {
        storage.delete(event.publicId());
    }
}
