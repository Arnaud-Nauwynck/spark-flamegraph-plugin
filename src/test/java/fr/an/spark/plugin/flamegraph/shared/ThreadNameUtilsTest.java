package fr.an.spark.plugin.flamegraph.shared;

import org.junit.Assert;
import org.junit.Test;

public class ThreadNameUtilsTest {

    @Test
    public void testTemplatizeThreadName() {
        assertTemplatize("test", "test");
        assertTemplatize("test-*", "test-1");
        assertTemplatize("test-*", "test-1234");
        assertTemplatize("test-*-*", "test-1-123");
        assertTemplatize("test-*-*", "test-123-123");
    }

    private void assertTemplatize(String expectedTemplatizeName, String threaName) {
        String actualTemplatized = ThreadNameUtils.templatizeThreadName(threaName);
        Assert.assertEquals(expectedTemplatizeName, actualTemplatized);
    }


}
