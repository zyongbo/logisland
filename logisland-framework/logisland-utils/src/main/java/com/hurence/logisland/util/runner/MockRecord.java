/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.hurence.logisland.util.runner;

import com.hurence.logisland.record.Record;
import com.hurence.logisland.record.StandardRecord;
import org.junit.Assert;

public class MockRecord extends StandardRecord {


    public MockRecord(Record toClone) {
        super(toClone);
    }

    public void assertFieldExists(final String fieldName) {
        Assert.assertTrue("Field " + fieldName + " does not exist", hasField(fieldName));
    }

    public void assertFieldNotExists(final String fieldName) {
        Assert.assertFalse("Attribute " + fieldName + " not exists with value " + getField(fieldName),
                hasField(fieldName));
    }

    public void assertFieldEquals(final String fieldName, final String expectedValue) {
        Assert.assertEquals(expectedValue, getField(fieldName).asString());
    }

    public void assertFieldNotEquals(final String fieldName, final String expectedValue) {
        Assert.assertNotSame(expectedValue, getField(fieldName).asString());
    }


    public void assertRecordSizeEquals(final int size) {
        Assert.assertEquals(size, size());
    }


    /**
     * Asserts that the content of this FlowFile is the same as the content of
     * the given file
     *
     * @param record to compare content against
     */
    public void assertContentEquals(final Record record) {
        Assert.assertEquals(this, record);
    }

}
