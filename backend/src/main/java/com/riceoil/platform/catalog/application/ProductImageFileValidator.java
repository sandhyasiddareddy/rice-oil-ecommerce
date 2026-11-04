package com.riceoil.platform.catalog.application;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Arrays;

final class ProductImageFileValidator {

    static final long MAX_FILE_SIZE_BYTES = 5 * 1024 * 1024;

    private ProductImageFileValidator() {
    }

    static ValidatedImage validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("An image file is required");
        }
        if (file.getSize() > MAX_FILE_SIZE_BYTES) {
            throw new IllegalArgumentException("Image file exceeds the 5 MB limit");
        }

        byte[] content;
        try {
            content = file.getBytes();
        } catch (IOException exception) {
            throw new IllegalStateException("Could not read uploaded image", exception);
        }

        String contentType = detectContentType(content);
        if (!contentType.equalsIgnoreCase(file.getContentType())) {
            throw new IllegalArgumentException("Image content does not match its declared content type");
        }

        String extension = switch (contentType) {
            case "image/jpeg" -> "jpg";
            case "image/png" -> "png";
            case "image/webp" -> "webp";
            default -> throw new IllegalArgumentException("Only JPEG, PNG, and WebP images are supported");
        };
        return new ValidatedImage(content, contentType, extension);
    }

    private static String detectContentType(byte[] content) {
        if (content.length >= 8 && Arrays.equals(
                Arrays.copyOf(content, 8),
                new byte[]{(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A})) {
            return "image/png";
        }
        if (content.length >= 3
                && content[0] == (byte) 0xFF
                && content[1] == (byte) 0xD8
                && content[2] == (byte) 0xFF) {
            return "image/jpeg";
        }
        if (content.length >= 12
                && content[0] == 'R'
                && content[1] == 'I'
                && content[2] == 'F'
                && content[3] == 'F'
                && content[8] == 'W'
                && content[9] == 'E'
                && content[10] == 'B'
                && content[11] == 'P') {
            return "image/webp";
        }
        throw new IllegalArgumentException("Uploaded file is not a supported image");
    }

    record ValidatedImage(byte[] content, String contentType, String extension) {
    }
}
