package org.example.Service;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.auth0.jwt.interfaces.JWTVerifier;

import com.auth0.jwt.JWTCreator;
import org.example.FeatureFlag;
import org.example.SoftwareVersion;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.Key;
import java.time.LocalDate;
import java.util.Base64;
import java.util.List;
import java.util.Random;

public class LicenseManager {
    // Секретный ключ для JWT (HMAC-SHA256)
    private static final String JWT_SECRET_KEY = "zAJcJOgo0mBAQEzwWXHDxyahBqn0uLja++zpUdbzC1YHZXMZyZcMWa0SCttW19xs";
    private static final Algorithm JWT_ALGORITHM = Algorithm.HMAC256(JWT_SECRET_KEY);

    // Секретный ключ для AES-шифрования (должен быть 16, 24 или 32 байта)
    private static final String AES_SECRET_KEY = "16byteAESkey1234"; // 16 байт
    private static final String LICENSE_FILE = System.getProperty("user.home") + "/AppData/Roaming/FastMarking/license.txt"; // Файл лицензии

    // Генерация лицензионного ключа
    public static String generateLicenseKey(LocalDate expirationDate) {
        return generateLicenseKey(new LicenseRequest(expirationDate, SoftwareVersion.V1_01, List.of(), null));
    }

    public static String generateLicenseKey(LicenseRequest request) {
        System.out.println("[INFO] Генерация лицензионного ключа...");
        String code = generateRandomCode(8); // Генерируем код
        JWTCreator.Builder builder = JWT.create()
                .withClaim("expiration", request.expirationDate().toString())
                .withClaim("code", code)
                .withClaim("version", request.version().getCode());

        if (request.company() != null) {
            builder.withClaim("company", request.company());
        }

        if (!request.features().isEmpty()) {
            String[] features = request.features().stream()
                    .map(FeatureFlag::name)
                    .toArray(String[]::new);
            builder.withArrayClaim("features", features);
        }

        String licenseKey = builder.sign(JWT_ALGORITHM);
        System.out.println("[INFO] Лицензионный ключ успешно сгенерирован: " + licenseKey);
        return licenseKey;
    }

    // Метод для получения кода из лицензии
    public static String getLicenseCode(String licenseKey) {
        try {
            DecodedJWT jwt = JWT.decode(licenseKey);
            return jwt.getClaim("code").asString();
        } catch (Exception e) {
            return null;
        }
    }
    private static String generateRandomCode(int length) {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        StringBuilder sb = new StringBuilder();
        Random random = new Random();
        for (int i = 0; i < length; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString();
    }

    // Проверка лицензионного ключа
    public static boolean validateLicenseKey(String licenseKey) {
        System.out.println("[INFO] Проверка лицензионного ключа...");
        try {
            JWTVerifier verifier = JWT.require(JWT_ALGORITHM).build();
            DecodedJWT jwt = verifier.verify(licenseKey);

            // Получаем дату окончания лицензии
            String expirationDateStr = jwt.getClaim("expiration").asString();
            LocalDate expirationDate = LocalDate.parse(expirationDateStr);
            System.out.println("[INFO] Дата окончания лицензии: " + expirationDate);

            // Проверяем, не истекла ли лицензия
            boolean isValid = !LocalDate.now().isAfter(expirationDate);
            System.out.println("[INFO] Лицензия " + (isValid ? "действительна" : "недействительна"));
            return isValid;
        } catch (JWTVerificationException e) {
            System.out.println("[ERROR] Лицензионный ключ недействителен: " + e.getMessage());
            return false; // Ключ недействителен
        }
    }

    // Шифрование данных лицензии
    public static String encrypt(String data) throws Exception {
        System.out.println("[INFO] Шифрование данных лицензии...");
        Key key = new SecretKeySpec(AES_SECRET_KEY.getBytes(StandardCharsets.UTF_8), "AES");
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.ENCRYPT_MODE, key);
        byte[] encryptedBytes = cipher.doFinal(data.getBytes(StandardCharsets.UTF_8));
        String encryptedData = Base64.getEncoder().encodeToString(encryptedBytes);
        System.out.println("[INFO] Данные успешно зашифрованы.");
        return encryptedData;
    }

    // Расшифровка данных лицензии
    public static String decrypt(String encryptedData) throws Exception {
        System.out.println("[INFO] Расшифровка данных лицензии...");
        Key key = new SecretKeySpec(AES_SECRET_KEY.getBytes(StandardCharsets.UTF_8), "AES");
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.DECRYPT_MODE, key);
        byte[] decryptedBytes = cipher.doFinal(Base64.getDecoder().decode(encryptedData));
        String decryptedData = new String(decryptedBytes, StandardCharsets.UTF_8);
        System.out.println("[INFO] Данные успешно расшифрованы.");
        return decryptedData;
    }

    // Сохранение лицензии в файл
    public static void saveLicense(String licenseKey) throws Exception {
        System.out.println("[INFO] Сохранение лицензии в файл...");
        String encryptedLicense = encrypt(licenseKey);
        Path licensePath = Paths.get(LICENSE_FILE);
        Files.createDirectories(licensePath.getParent()); // Создаём папку, если её нет
        Files.write(licensePath, encryptedLicense.getBytes(StandardCharsets.UTF_8));
        System.out.println("[INFO] Лицензия успешно сохранена в файл: " + licensePath.toAbsolutePath());
    }

    // Загрузка лицензии из файла
    public static String loadLicense() throws Exception {
        System.out.println("[INFO] Загрузка лицензии из файла...");
        Path licensePath = Paths.get(LICENSE_FILE);
        if (!Files.exists(licensePath)) {
            System.out.println("[WARN] Файл лицензии не существует: " + licensePath.toAbsolutePath());
            return null; // Файл лицензии не существует
        }
        byte[] encryptedBytes = Files.readAllBytes(licensePath);
        String encryptedLicense = new String(encryptedBytes, StandardCharsets.UTF_8);
        String licenseKey = decrypt(encryptedLicense);
        System.out.println("[INFO] Лицензия успешно загружена из файла.");
        return licenseKey;
    }

    public static LocalDate getExpirationDate(String licenseKey) {
        try {
            DecodedJWT jwt = JWT.decode(licenseKey);
            return LocalDate.parse(jwt.getClaim("expiration").asString());
        } catch (Exception e) {
            return null;
        }
    }
}