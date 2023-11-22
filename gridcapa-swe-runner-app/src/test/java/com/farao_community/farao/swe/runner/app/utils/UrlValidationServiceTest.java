package com.farao_community.farao.swe.runner.app.utils;

import com.farao_community.farao.gridcapa_swe_commons.exception.SweInvalidDataException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
class UrlValidationServiceTest {

    @Autowired
    private UrlValidationService urlValidationService;

    private final String nonWhitelistedUrl = "http://www.notwhitelisted.com";

    @Test
    void testOpenUrlStreamUrlIsNotPartOfWhitelistShouldThrowException() {
        assertThrows(SweInvalidDataException.class, () -> {
            urlValidationService.openUrlStream(nonWhitelistedUrl);
        });
    }
}
