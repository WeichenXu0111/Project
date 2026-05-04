package org.example.security;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PasswordUtilTest {
    @Test
    void hashAndVerifyPassword() {
        String salt = PasswordUtil.generateSalt();
        String hash = PasswordUtil.hashPassword("StrongPass123", salt);

        assertTrue(PasswordUtil.verifyPassword("StrongPass123", salt, hash));
        assertFalse(PasswordUtil.verifyPassword("WrongPass", salt, hash));
    }

    @Test
    void generateSalt_ReturnsUniqueAndNonEmptyString() {
        String salt1 = PasswordUtil.generateSalt();
        String salt2 = PasswordUtil.generateSalt();
        
        assertNotNull(salt1);
        assertFalse(salt1.isEmpty());
        assertNotEquals(salt1, salt2, "Two generated salts should not be equal");
    }
}

