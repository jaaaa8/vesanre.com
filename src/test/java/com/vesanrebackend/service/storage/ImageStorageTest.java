package com.vesanrebackend.service.storage;

import com.cloudinary.Cloudinary;
import com.cloudinary.Uploader;
import com.cloudinary.utils.ObjectUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ImageStorageTest {
    static final byte[] JPEG = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0, 0x10};
    static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 0, 0};
    static final byte[] WEBP = {'R', 'I', 'F', 'F', 0, 0, 0, 0, 'W', 'E', 'B', 'P', 'V', 'P'};

    Cloudinary cloudinary = mock(Cloudinary.class);
    Uploader uploader = mock(Uploader.class);
    ImageStorage storage = new ImageStorage(cloudinary);

    @BeforeEach
    void setUp() throws IOException {
        when(cloudinary.uploader()).thenReturn(uploader);
        when(uploader.upload(any(), anyMap())).thenAnswer(inv -> Map.of("public_id", ((Map<?, ?>) inv.getArgument(1)).get("public_id")));
    }

    @Test
    void validImagesUploadIntoFolderWithAllowedFormats() throws IOException {
        for (byte[] bytes : new byte[][]{JPEG, PNG, WEBP}) {
            String id = storage.upload(new MockMultipartFile("file", "x.bin", "application/octet-stream", bytes), "vesanre/venues/v1");
            assertThat(id).startsWith("vesanre/venues/v1/").hasSize("vesanre/venues/v1/".length() + 36);
        }
        ArgumentCaptor<Map> options = ArgumentCaptor.forClass(Map.class);
        verify(uploader, org.mockito.Mockito.times(3)).upload(any(), options.capture());
        assertThat(options.getValue()).containsEntry("allowed_formats", "jpg,png,webp")
                .containsEntry("resource_type", "image").containsEntry("overwrite", false);
    }

    @Test
    void fakeOrEmptyFilesAreRejectedBeforeNetwork() throws IOException {
        byte[] text = "hello, not an image".getBytes(StandardCharsets.UTF_8);
        byte[] svg = "<svg xmlns=\"http://www.w3.org/2000/svg\"/>".getBytes(StandardCharsets.UTF_8);
        for (MockMultipartFile bad : new MockMultipartFile[]{
                new MockMultipartFile("file", "a.jpg", "image/jpeg", text),
                new MockMultipartFile("file", "a.svg", "image/svg+xml", svg),
                new MockMultipartFile("file", "a.png", "image/png", new byte[0]),
                new MockMultipartFile("file", "a.png", "image/png", new byte[]{(byte) 0x89, 'P'})}) {
            assertStatus(() -> storage.upload(bad, "vesanre/venues/v1"), 400);
        }
        assertStatus(() -> storage.upload(null, "vesanre/venues/v1"), 400);
        verify(uploader, never()).upload(any(), anyMap());
    }

    @Test
    void cloudinaryFailureIsBadGateway() throws IOException {
        doThrow(new IOException("down")).when(uploader).upload(any(), anyMap());
        assertStatus(() -> storage.upload(new MockMultipartFile("file", JPEG), "f"), 502);
        doThrow(new RuntimeException("Invalid image file")).when(uploader).upload(any(), anyMap());
        assertStatus(() -> storage.upload(new MockMultipartFile("file", JPEG), "f"), 502);
    }

    @Test
    void deleteNeverThrows() throws IOException {
        when(uploader.destroy(eq("a"), anyMap())).thenReturn(Map.of("result", "not found"));
        storage.delete("a");
        when(uploader.destroy(eq("b"), anyMap())).thenThrow(new IOException("down"));
        storage.delete("b");
        verify(uploader).destroy(eq("a"), eq(ObjectUtils.asMap("resource_type", "image", "invalidate", true)));
    }

    @Test
    void urlIsBuiltLocallyFromPublicId() {
        ImageStorage real = new ImageStorage(new Cloudinary(ObjectUtils.asMap("cloud_name", "demo", "secure", true)));
        assertThat(real.url("vesanre/venues/v1/abc")).startsWith("https://res.cloudinary.com/demo/image/upload/")
                .contains("/vesanre/venues/v1/abc"); // SDK may append an analytics query
        assertThat(real.url(null)).isNull();
    }

    private static void assertStatus(Runnable call, int status) {
        assertThatThrownBy(call::run).isInstanceOfSatisfying(ResponseStatusException.class,
                e -> assertThat(e.getStatusCode().value()).isEqualTo(status));
    }
}
