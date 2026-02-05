package com.microsoft.azure.storagev10.advanced;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import static org.junit.Assert.assertTrue;

/**
 * Unit tests for ConcurrentUploadRunner.
 */
public class ConcurrentUploadRunnerTest {

    private final ByteArrayOutputStream outputContent = new ByteArrayOutputStream();
    private final PrintStream originalOut = System.out;
    private String originalAccountName;
    private String originalAccountKey;

    @Before
    public void setUp() {
        System.setOut(new PrintStream(outputContent));
        originalAccountName = System.getenv("AZURE_STORAGE_ACCOUNT_NAME");
        originalAccountKey = System.getenv("AZURE_STORAGE_ACCOUNT_KEY");
    }

    @After
    public void tearDown() {
        System.setOut(originalOut);
    }

    @Test
    public void testMainWithoutEnvironmentVariables() {
        // Clear environment variables by running with null values
        String[] args = new String[0];
        
        // Should print message about missing environment variables
        ConcurrentUploadRunner.main(args);
        
        String output = outputContent.toString();
        assertTrue(output.contains("Please set AZURE_STORAGE_ACCOUNT_NAME and AZURE_STORAGE_ACCOUNT_KEY"));
    }

    @Test
    public void testMainMethodExists() {
        // Verify main method is present and can be called
        try {
            ConcurrentUploadRunner.class.getMethod("main", String[].class);
        } catch (NoSuchMethodException e) {
            throw new AssertionError("main method should exist", e);
        }
    }
}
