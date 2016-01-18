package com.happen.it.make.whatisit;

import android.app.Application;
import android.content.Context;
import android.test.AndroidTestCase;
import android.test.ApplicationTestCase;
import android.test.InstrumentationTestCase;
import android.test.mock.MockContext;

/**
 * <a href="http://d.android.com/tools/testing/testing_android.html">Testing Fundamentals</a>
 */
public class ApplicationTest extends InstrumentationTestCase {
    public Context context;

    public void setUp() throws Exception {
        super.setUp();
//
        context = getInstrumentation().getContext();
//
//        assertNotNull(context);
    }

    public void testMxNetForwardPass() {
        MxNetGauge mxNetGauge = new MxNetGauge(context, 10);
        mxNetGauge.runTest();
        System.out.println(mxNetGauge.toString());
    }
}