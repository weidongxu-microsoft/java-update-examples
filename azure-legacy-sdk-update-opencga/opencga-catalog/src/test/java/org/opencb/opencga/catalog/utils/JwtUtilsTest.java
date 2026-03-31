package org.opencb.opencga.catalog.utils;

import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.opencb.opencga.core.common.JwtUtils;
import org.opencb.opencga.core.testclassification.duration.ShortTests;

@Category(ShortTests.class)
public class JwtUtilsTest {

    @Test
    public void getExpirationDateTest() {
        String token = "<REDACTED_JWT_TOKEN>";
        JwtUtils.getExpirationDate(token);
    }
}
