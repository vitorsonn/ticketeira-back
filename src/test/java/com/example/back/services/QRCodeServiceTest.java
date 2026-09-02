package com.example.back.services;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;

class QRCodeServiceTest {

    private final QRCodeService qrCodeService = new QRCodeService();

    @Test
    @DisplayName("Deve gerar uma imagem Base64 válida a partir de um texto")
    void generateQRcodeBase64_Success() throws Exception {
        // Arrange
        String content = "ticket-uuid-123456";

        // Act
        String base64Result = qrCodeService.generateQRcodeBase64(content);

        // Assert
        assertNotNull(base64Result);
        assertFalse(base64Result.isBlank());

        // Valida se o retorno é um Base64 decodificável
        byte[] decodedBytes = Base64.getDecoder().decode(base64Result);
        assertTrue(decodedBytes.length > 0);
    }
}
