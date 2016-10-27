package com.jivesoftware.os.routing.bird.deployable;

import com.jivesoftware.os.routing.bird.deployable.AuthValidationFilter.NoAuthEvaluator;
import com.jivesoftware.os.routing.bird.deployable.AuthValidationFilter.PathedAuthEvaluator;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 *
 */
public class AuthValidationFilterTest {

    @Test
    public void testNoWildcard() throws Exception {
        PathedAuthEvaluator pathedAuthEvaluator = new PathedAuthEvaluator(new NoAuthEvaluator(), "/a/b");
        Assert.assertTrue(pathedAuthEvaluator.matches("/a/b"));
        Assert.assertFalse(pathedAuthEvaluator.matches("/"));
        Assert.assertFalse(pathedAuthEvaluator.matches("/a"));
        Assert.assertFalse(pathedAuthEvaluator.matches("/a/"));
        Assert.assertFalse(pathedAuthEvaluator.matches("/a/b/"));
        Assert.assertFalse(pathedAuthEvaluator.matches("/a/b/c"));
    }

    @Test
    public void testWildcard() throws Exception {
        PathedAuthEvaluator pathedAuthEvaluator = new PathedAuthEvaluator(new NoAuthEvaluator(), "/a/*");
        Assert.assertTrue(pathedAuthEvaluator.matches("/a"));
        Assert.assertTrue(pathedAuthEvaluator.matches("/a/"));
        Assert.assertTrue(pathedAuthEvaluator.matches("/a/b"));
        Assert.assertFalse(pathedAuthEvaluator.matches("/"));
        Assert.assertFalse(pathedAuthEvaluator.matches("/ab"));
        Assert.assertFalse(pathedAuthEvaluator.matches("/b"));
    }

    @Test
    public void testRootWildcard() throws Exception {
        PathedAuthEvaluator pathedAuthEvaluator = new PathedAuthEvaluator(new NoAuthEvaluator(), "/*");
        Assert.assertTrue(pathedAuthEvaluator.matches("/"));
        Assert.assertTrue(pathedAuthEvaluator.matches("/a/"));
        Assert.assertTrue(pathedAuthEvaluator.matches("/a/b"));
    }
}