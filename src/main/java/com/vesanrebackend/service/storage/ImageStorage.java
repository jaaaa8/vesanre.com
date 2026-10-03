package com.vesanrebackend.service.storage;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;

// Cloudinary wrapper. The DB keeps only public_id; URLs are always rebuilt from it (spec D4).
@Service
public class ImageStorage {
    private static final Logger log = LoggerFactory.getLogger(ImageStorage.class);

    private final Cloudinary cloudinary;

    public ImageStorage(Cloudinary cloudinary) {
        this.cloudinary = cloudinary;
    }

    // Trusts content (magic bytes), never the client's Content-Type or file name. Returns the public_id.
    public String upload(MultipartFile file, String folder) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "file is required");
        }
        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot read file");
        }
        if (!isImage(bytes)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only JPEG, PNG or WebP images are allowed");
        }
        try {
            Map<?, ?> result = cloudinary.uploader().upload(bytes, ObjectUtils.asMap(
                    "public_id", folder + "/" + UUID.randomUUID(), "resource_type", "image",
                    "allowed_formats", "jpg,png,webp", "overwrite", false));
            return (String) result.get("public_id");
        } catch (IOException | RuntimeException e) {
            // A corrupt file with a valid header also lands here (spec §5: accepted as 502).
            log.warn("Image upload to {} failed", folder, e);
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Image storage is unavailable");
        }
    }

    public String url(String publicId) {
        return publicId == null ? null : cloudinary.url().secure(true).generate(publicId);
    }

    // Best effort: never throws, runs after commit/rollback.
    public void delete(String publicId) {
        try {
            Map<?, ?> result = cloudinary.uploader().destroy(publicId,
                    ObjectUtils.asMap("resource_type", "image", "invalidate", true));
            Object status = result.get("result");
            if (!"ok".equals(status) && !"not found".equals(status)) {
                log.warn("Image delete {} returned {}", publicId, status);
            }
        } catch (IOException | RuntimeException e) {
            log.warn("Image delete {} failed", publicId, e);
        }
    }

    private static boolean isImage(byte[] b) {
        return startsWith(b, 0, 0xFF, 0xD8, 0xFF)
                || startsWith(b, 0, 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A)
                || (startsWith(b, 0, 'R', 'I', 'F', 'F') && startsWith(b, 8, 'W', 'E', 'B', 'P'));
    }

    private static boolean startsWith(byte[] b, int offset, int... signature) {
        if (b.length < offset + signature.length) {
            return false;
        }
        for (int i = 0; i < signature.length; i++) {
            if ((b[offset + i] & 0xFF) != signature[i]) {
                return false;
            }
        }
        return true;
    }
}
