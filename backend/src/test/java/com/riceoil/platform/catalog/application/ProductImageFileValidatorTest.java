package com.riceoil.platform.catalog.application;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ProductImageFileValidatorTest {

    @Test
    void validate_shouldAcceptPngWithMatchingContentType() {
        byte[] pngHeader = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A};
        MockMultipartFile file = new MockMultipartFile("file", "product.png", "image/png", pngHeader);

        ProductImageFileValidator.ValidatedImage image = ProductImageFileValidator.validate(file);

        assertEquals("image/png", image.contentType());
        assertEquals("png", image.extension());
    }

    @Test
    void validate_shouldRejectMismatchedContentType() {
        byte[] pngHeader = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A};
        MockMultipartFile file = new MockMultipartFile("file", "product.jpg", "image/jpeg", pngHeader);

        assertThrows(IllegalArgumentException.class, () -> ProductImageFileValidator.validate(file));
    }

    @Test
    void validate_shouldRejectUnsupportedFileContent() {
        MockMultipartFile file = new MockMultipartFile("file", "product.svg", "image/svg+xml",
                "<svg></svg>".getBytes());

        assertThrows(IllegalArgumentException.class, () -> ProductImageFileValidator.validate(file));
    }

    @Test
    void validate_shouldRejectOversizedImages() {
        byte[] content = new byte[(int) ProductImageFileValidator.MAX_FILE_SIZE_BYTES + 1];
        MockMultipartFile file = new MockMultipartFile("file", "large.png", "image/png", content);

        assertThrows(IllegalArgumentException.class, () -> ProductImageFileValidator.validate(file));
    }
}
