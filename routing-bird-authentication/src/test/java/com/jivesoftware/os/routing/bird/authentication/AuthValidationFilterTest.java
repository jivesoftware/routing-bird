package com.jivesoftware.os.routing.bird.authentication;

import com.jivesoftware.os.routing.bird.authentication.NoAuthEvaluator;
import com.jivesoftware.os.routing.bird.authentication.AuthValidationFilter.PathedAuthEvaluator;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 *
 */
public class AuthValidationFilterTest {

    @Test
    public void testRootPath() throws Exception {
        PathedAuthEvaluator pathedAuthEvaluator = new PathedAuthEvaluator(new NoAuthEvaluator(), "/");
        Assert.assertTrue(pathedAuthEvaluator.matches("/"));
        Assert.assertFalse(pathedAuthEvaluator.matches("/a"));
        Assert.assertFalse(pathedAuthEvaluator.matches("/a/"));
        Assert.assertFalse(pathedAuthEvaluator.matches("/a/b/"));
        Assert.assertFalse(pathedAuthEvaluator.matches("/a/b/c"));
    }

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