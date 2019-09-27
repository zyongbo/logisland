/**
 * Copyright (C) 2016 Hurence (support@hurence.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.hurence.logisland.rest.processor.lookup;

import com.hurence.logisland.component.InitializationException;
import com.hurence.logisland.record.FieldType;
import com.hurence.logisland.record.StandardRecord;
import com.hurence.logisland.service.lookup.LookupFailureException;
import com.hurence.logisland.service.rest.MockRestClientService;
import com.hurence.logisland.service.rest.RestClientService;
import com.hurence.logisland.util.runner.MockRecord;
import com.hurence.logisland.util.runner.TestRunner;
import com.hurence.logisland.util.runner.TestRunners;
import org.junit.Test;

import java.io.IOException;

import static com.hurence.logisland.rest.processor.lookup.CallRequest.*;

public class CallRequestTest {

    private static String SERVICE_ID = "restLookupService";
    @Test
    public void testCustomValidate() {
        final TestRunner runner = TestRunners.newTestRunner(new CallRequest());
        runner.setProcessorIdentifier("test proc");
        runner.setProperty(HTTP_CLIENT_SERVICE, "restLookupService");
        runner.assertValid();
        runner.setProperty(FIELD_HTTP_RESPONSE, "response");
        runner.assertValid();
        runner.setProperty(REQUEST_METHOD, "get");
        runner.assertValid();
        runner.setProperty(REQUEST_METHOD, "${method}");
        runner.assertValid();
        runner.setProperty(REQUEST_MIME_TYPE, "text/csv");
        runner.assertValid();
        runner.setProperty(REQUEST_MIME_TYPE, "${mime_type}");
        runner.assertValid();
        runner.setProperty(REQUEST_BODY, "my raw body string");
        runner.assertValid();
        runner.setProperty(REQUEST_BODY, "${body_field}");
        runner.assertValid();
        runner.setProperty(CONFLICT_RESOLUTION_POLICY, OVERWRITE_EXISTING.getValue());
        runner.assertValid();
        runner.setProperty(CONFLICT_RESOLUTION_POLICY, KEEP_OLD_FIELD.getValue());
        runner.assertValid();
        runner.setProperty(CONFLICT_RESOLUTION_POLICY, "other");
        runner.assertNotValid();
    }

    @Test
    public void basic_test() throws InitializationException {
        final TestRunner runner = getRunnerInitialized();

        //test queries
        StandardRecord record1 = new StandardRecord();
        record1.setField("employeeId", FieldType.INT, 1);
        StandardRecord record2 = new StandardRecord();
        record2.setField("employeeId", FieldType.INT, 2);
        runner.enqueue(record1, record2);
        runner.run();
        runner.assertAllInputRecordsProcessed();
        runner.assertOutputRecordsCount(2);

        MockRecord out = runner.getOutputRecords().get(0);
        out.assertRecordSizeEquals(2);
        out.assertFieldEquals("employeeId", 1);
        out.assertFieldTypeEquals("employeeId", FieldType.INT);
        out.assertFieldTypeEquals("response", FieldType.RECORD);
        MockRecord coordinnates = new MockRecord(out.getField("response").asRecord());
        coordinnates.assertRecordSizeEquals(1);
        coordinnates.assertFieldEquals("employeeId", 1);
        coordinnates.assertFieldTypeEquals("employeeId", FieldType.INT);
        MockRecord out2 = runner.getOutputRecords().get(1);
        out2.assertFieldEquals("employeeId", 2);
        out2.assertFieldTypeEquals("employeeId", FieldType.INT);
        out2.assertFieldTypeEquals("response", FieldType.RECORD);
        MockRecord coordinnates2 = new MockRecord(out2.getField("response").asRecord());
        coordinnates2.assertRecordSizeEquals(1);
        coordinnates2.assertFieldEquals("employeeId", 2);
        coordinnates2.assertFieldTypeEquals("employeeId", FieldType.INT);
    }

    @Test
    public void basic_test_2() throws InitializationException, IOException, LookupFailureException {
        final TestRunner runner = getRunnerInitialized();

        //test queries
        StandardRecord record = new StandardRecord();
        record.setField("employeeId", FieldType.STRING, "hello");
        runner.enqueue(new StandardRecord(record));
        runner.run();
        runner.assertAllInputRecordsProcessed();
        runner.assertOutputRecordsCount(1);

        MockRecord out = runner.getOutputRecords().get(0);
        out.assertRecordSizeEquals(2);
        out.assertFieldEquals("employeeId", "hello");
        out.assertFieldTypeEquals("employeeId", FieldType.STRING);
        out.assertFieldTypeEquals("response", FieldType.RECORD);
        MockRecord coordinnates = new MockRecord(out.getField("response").asRecord());
        coordinnates.assertRecordSizeEquals(1);
        coordinnates.assertFieldEquals("employeeId", "hello");
        coordinnates.assertFieldTypeEquals("employeeId", FieldType.STRING);
    }

    @Test
    public void adding_verb_to_coordinates() throws InitializationException, IOException, LookupFailureException {
        final TestRunner runner = getRunnerInitialized();
        final RestClientService service = (RestClientService) runner.getControllerService(SERVICE_ID);
        runner.setProperty(REQUEST_METHOD, "delete");
        runner.assertValid();

        //test queries
        StandardRecord record = new StandardRecord();
        record.setField("employeeId", FieldType.STRING, "hello");
        runner.enqueue(new StandardRecord(record));
        runner.run();
        runner.assertAllInputRecordsProcessed();
        runner.assertOutputRecordsCount(1);

        MockRecord out = runner.getOutputRecords().get(0);
        out.assertRecordSizeEquals(2);
        out.assertFieldEquals("employeeId", "hello");
        out.assertFieldTypeEquals("employeeId", FieldType.STRING);
        out.assertFieldTypeEquals("response", FieldType.RECORD);
        MockRecord coordinates = new MockRecord(out.getField("response").asRecord());
        coordinates.assertRecordSizeEquals(2);
        coordinates.assertFieldEquals("employeeId", "hello");
        coordinates.assertFieldEquals(service.getMethodKey(), "delete");
        coordinates.assertFieldTypeEquals("employeeId", FieldType.STRING);
    }

    @Test
    public void adding_verb_to_coordinates_expression_language() throws InitializationException, IOException, LookupFailureException {
        final TestRunner runner = getRunnerInitialized();
        final RestClientService service = (RestClientService) runner.getControllerService(SERVICE_ID);
        runner.setProperty(REQUEST_METHOD, "${'delete_' + employeeId}");
        runner.assertValid();

        //test queries
        StandardRecord record = new StandardRecord();
        record.setField("employeeId", FieldType.STRING, "hello");
        runner.enqueue(new StandardRecord(record));
        runner.run();
        runner.assertAllInputRecordsProcessed();
        runner.assertOutputRecordsCount(1);

        MockRecord out = runner.getOutputRecords().get(0);
        out.assertRecordSizeEquals(2);
        out.assertFieldEquals("employeeId", "hello");
        out.assertFieldTypeEquals("employeeId", FieldType.STRING);
        out.assertFieldTypeEquals("response", FieldType.RECORD);
        MockRecord coordinates = new MockRecord(out.getField("response").asRecord());
        coordinates.assertRecordSizeEquals(2);
        coordinates.assertFieldEquals("employeeId", "hello");
        coordinates.assertFieldEquals(service.getMethodKey(), "delete_hello");
        coordinates.assertFieldTypeEquals("employeeId", FieldType.STRING);
    }

    @Test
    public void adding_mime_type_coordinates() throws InitializationException, IOException, LookupFailureException {
        final TestRunner runner = getRunnerInitialized();
        final RestClientService service = (RestClientService) runner.getControllerService(SERVICE_ID);
        runner.setProperty(REQUEST_MIME_TYPE, "mimtype");
        runner.assertValid();

        //test queries
        StandardRecord record = new StandardRecord();
        record.setField("employeeId", FieldType.STRING, "hello");
        runner.enqueue(new StandardRecord(record));
        runner.run();
        runner.assertAllInputRecordsProcessed();
        runner.assertOutputRecordsCount(1);

        MockRecord out = runner.getOutputRecords().get(0);
        out.assertRecordSizeEquals(2);
        out.assertFieldEquals("employeeId", "hello");
        out.assertFieldTypeEquals("employeeId", FieldType.STRING);
        out.assertFieldTypeEquals("response", FieldType.RECORD);
        MockRecord coordinates = new MockRecord(out.getField("response").asRecord());
        coordinates.assertRecordSizeEquals(2);
        coordinates.assertFieldEquals("employeeId", "hello");
        coordinates.assertFieldEquals(service.getMimeTypeKey(), "mimtype");
        coordinates.assertFieldTypeEquals("employeeId", FieldType.STRING);
    }

    @Test
    public void adding_mime_type_coordinates_expression_language() throws InitializationException, IOException, LookupFailureException {
        final TestRunner runner = getRunnerInitialized();
        final RestClientService service = (RestClientService) runner.getControllerService(SERVICE_ID);
        runner.setProperty(REQUEST_MIME_TYPE, "${'mimtype_' + employeeId}");
        runner.assertValid();

        //test queries
        StandardRecord record = new StandardRecord();
        record.setField("employeeId", FieldType.STRING, "hello");
        runner.enqueue(new StandardRecord(record));
        runner.run();
        runner.assertAllInputRecordsProcessed();
        runner.assertOutputRecordsCount(1);

        MockRecord out = runner.getOutputRecords().get(0);
        out.assertRecordSizeEquals(2);
        out.assertFieldEquals("employeeId", "hello");
        out.assertFieldTypeEquals("employeeId", FieldType.STRING);
        out.assertFieldTypeEquals("response", FieldType.RECORD);
        MockRecord coordinates = new MockRecord(out.getField("response").asRecord());
        coordinates.assertRecordSizeEquals(2);
        coordinates.assertFieldEquals("employeeId", "hello");
        coordinates.assertFieldEquals(service.getMimeTypeKey(), "mimtype_hello");
        coordinates.assertFieldTypeEquals("employeeId", FieldType.STRING);
    }

    @Test
    public void adding_body_coordinates() throws InitializationException, IOException, LookupFailureException {
        final TestRunner runner = getRunnerInitialized();
        final RestClientService service = (RestClientService) runner.getControllerService(SERVICE_ID);
        runner.setProperty(REQUEST_BODY, "body");
        runner.assertValid();

        //test queries
        StandardRecord record = new StandardRecord();
        record.setField("employeeId", FieldType.STRING, "hello");
        runner.enqueue(new StandardRecord(record));
        runner.run();
        runner.assertAllInputRecordsProcessed();
        runner.assertOutputRecordsCount(1);

        MockRecord out = runner.getOutputRecords().get(0);
        out.assertRecordSizeEquals(2);
        out.assertFieldEquals("employeeId", "hello");
        out.assertFieldTypeEquals("employeeId", FieldType.STRING);
        out.assertFieldTypeEquals("response", FieldType.RECORD);
        MockRecord coordinates = new MockRecord(out.getField("response").asRecord());
        coordinates.assertRecordSizeEquals(2);
        coordinates.assertFieldEquals("employeeId", "hello");
        coordinates.assertFieldEquals(service.getbodyKey(), "body");
        coordinates.assertFieldTypeEquals("employeeId", FieldType.STRING);
    }

    @Test
    public void adding_body_coordinates_expression_language() throws InitializationException, IOException, LookupFailureException {
        final TestRunner runner = getRunnerInitialized();
        final RestClientService service = (RestClientService) runner.getControllerService(SERVICE_ID);
        runner.setProperty(REQUEST_BODY, "${'body_' + employeeId}");
        runner.assertValid();

        //test queries
        StandardRecord record = new StandardRecord();
        record.setField("employeeId", FieldType.STRING, "hello");
        runner.enqueue(new StandardRecord(record));
        runner.run();
        runner.assertAllInputRecordsProcessed();
        runner.assertOutputRecordsCount(1);

        MockRecord out = runner.getOutputRecords().get(0);
        out.assertRecordSizeEquals(2);
        out.assertFieldEquals("employeeId", "hello");
        out.assertFieldTypeEquals("employeeId", FieldType.STRING);
        out.assertFieldTypeEquals("response", FieldType.RECORD);
        MockRecord coordinates = new MockRecord(out.getField("response").asRecord());
        coordinates.assertRecordSizeEquals(2);
        coordinates.assertFieldEquals("employeeId", "hello");
        coordinates.assertFieldEquals(service.getbodyKey(), "body_hello");
        coordinates.assertFieldTypeEquals("employeeId", FieldType.STRING);
    }

    private TestRunner getRunnerInitialized() throws InitializationException {
        final TestRunner runner = TestRunners.newTestRunner(new CallRequest());
        MockRestClientService service = new MockRestClientService();
        //enable service
        runner.addControllerService("restLookupService", service);
        runner.enableControllerService(service);
        runner.assertValid(service);
        //config proc
        runner.setProperty(HTTP_CLIENT_SERVICE, "restLookupService");
        runner.setProperty(FIELD_HTTP_RESPONSE, "response");
        runner.setProperty(CONFLICT_RESOLUTION_POLICY, KEEP_OLD_FIELD.getValue());
        runner.assertValid();
        return runner;
    }
}
