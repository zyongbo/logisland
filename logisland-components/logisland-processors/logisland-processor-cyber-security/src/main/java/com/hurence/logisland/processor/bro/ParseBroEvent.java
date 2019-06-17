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
package com.hurence.logisland.processor.bro;

import com.hurence.logisland.annotation.documentation.*;
import com.hurence.logisland.component.PropertyDescriptor;
import com.hurence.logisland.processor.*;
import com.hurence.logisland.record.Field;
import com.hurence.logisland.record.FieldDictionary;
import com.hurence.logisland.record.FieldType;
import com.hurence.logisland.record.Record;
import com.hurence.logisland.util.string.JsonUtil;
import com.hurence.logisland.validator.StandardValidators;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Bro (https://www.bro.org/) processor
 */
@Category(ComponentCategory.SECURITY)
@Tags({"bro", "security", "IDS", "NIDS"})
@CapabilityDescription(
        "The ParseBroEvent processor is the Logisland entry point to get and process `Bro <https://www.bro.org>`_ events."
        + " The `Bro-Kafka plugin <https://github.com/bro/bro-plugins/tree/master/kafka>`_ should be used and configured"
        + " in order to have Bro events sent to Kafka."
        + " See the `Bro/Logisland tutorial <http://logisland.readthedocs.io/en/latest/tutorials/indexing-bro-events.html>`_"
        + " for an example of usage for this processor. The ParseBroEvent processor does some minor pre-processing on incoming Bro"
        + " events from the Bro-Kafka plugin to adapt them to Logisland.\n\n"
        + "Basically the events coming from the Bro-Kafka plugin are JSON documents with"
        + " a first level field indicating the type of the event. The ParseBroEvent processor takes the incoming JSON document,"
        + " sets the event type in a record_type field and sets the original sub-fields of the JSON event as first level"
        + " fields in the record. Also any dot in a field name is transformed into an underscore. Thus, for instance"
        + ", the field id.orig_h becomes id_orig_h. The next processors in the stream can then process the"
        + " Bro events generated by this ParseBroEvent processor.\n\n"
        + "As an example here is an incoming event from Bro:\n\n"
        + "{\n\n"
        + "   \"conn\": {\n\n"
        + "     \"id.resp_p\": 9092,\n\n"
        + "     \"resp_pkts\": 0,\n\n"
        + "     \"resp_ip_bytes\": 0,\n\n"
        + "     \"local_orig\": true,\n\n"
        + "     \"orig_ip_bytes\": 0,\n\n"
        + "     \"orig_pkts\": 0,\n\n"
        + "     \"missed_bytes\": 0,\n\n"
        + "     \"history\": \"Cc\",\n\n"
        + "     \"tunnel_parents\": [],\n\n"
        + "     \"id.orig_p\": 56762,\n\n"
        + "     \"local_resp\": true,\n\n"
        + "     \"uid\": \"Ct3Ms01I3Yc6pmMZx7\",\n\n"
        + "     \"conn_state\": \"OTH\",\n\n"
        + "     \"id.orig_h\": \"172.17.0.2\",\n\n"
        + "     \"proto\": \"tcp\",\n\n"
        + "     \"id.resp_h\": \"172.17.0.3\",\n\n"
        + "     \"ts\": 1487596886.953917\n\n"
        + "   }\n\n"
        + " }\n\n"
        + "It gets processed and transformed into the following Logisland record by the ParseBroEvent processor:\n\n"
        + "\"@timestamp\": \"2017-02-20T13:36:32Z\"\n\n"
        + "\"record_id\": \"6361f80a-c5c9-4a16-9045-4bb51736333d\"\n\n"
        + "\"record_time\": 1487597792782\n\n"
        + "\"record_type\": \"conn\"\n\n"
        + "\"id_resp_p\": 9092\n\n"
        + "\"resp_pkts\": 0\n\n"
        + "\"resp_ip_bytes\": 0\n\n"
        + "\"local_orig\": true\n\n"
        + "\"orig_ip_bytes\": 0\n\n"
        + "\"orig_pkts\": 0\n\n"
        + "\"missed_bytes\": 0\n\n"
        + "\"history\": \"Cc\"\n\n"
        + "\"tunnel_parents\": []\n\n"
        + "\"id_orig_p\": 56762\n\n"
        + "\"local_resp\": true\n\n"
        + "\"uid\": \"Ct3Ms01I3Yc6pmMZx7\"\n\n"
        + "\"conn_state\": \"OTH\"\n\n"
        + "\"id_orig_h\": \"172.17.0.2\"\n\n"
        + "\"proto\": \"tcp\"\n\n"
        + "\"id_resp_h\": \"172.17.0.3\"\n\n"
        + "\"ts\": 1487596886.953917")
@ExtraDetailFile("./details/ParseBroEvent-Detail.rst")
public class ParseBroEvent extends AbstractProcessor {

    private static Logger logger = LoggerFactory.getLogger(ParseBroEvent.class);

    private boolean debug = false;
    
    private static final String KEY_DEBUG = "debug";
    
    public static final PropertyDescriptor DEBUG = new PropertyDescriptor.Builder()
            .name(KEY_DEBUG)
            .description("Enable debug. If enabled, the original JSON string is embedded in the record_value field of the record.")
            .addValidator(StandardValidators.BOOLEAN_VALIDATOR)
            .required(false)
            .defaultValue("false")
            .build();

    private static final String FIELD_TS = "ts";
    private static final String FIELD_VERSION = "version";

    @Override
    public void init(final ProcessContext context)
    {

        super.init(context);
        logger.debug("Initializing Bro Processor");
    }
    
    @Override
    public List<PropertyDescriptor> getSupportedPropertyDescriptors() {
        
        final List<PropertyDescriptor> descriptors = new ArrayList<>();
        descriptors.add(DEBUG);

        return Collections.unmodifiableList(descriptors);
    }
  
    @Override
    public Collection<Record> process(ProcessContext context, Collection<Record> records)
    {
        if (debug)
        {
            logger.debug("Bro Processor records input: " + records);
        }

        /**
         * Get the original Bro event as a JSON string and do some adaptation:
         * - Bro field names with '.' are not acceptable for indexing into ES. Replace them with '_'.
         * - set the first level fields of the JSON Bro event as first level fields in the Logisland matching record 
         */
        for (Record record : records)
        {
            /**
             * First extract the first level field to get the Bro event type. Here is an example of a conn bro event:
             * 
             * {
             *   "conn": {
             *     "id.resp_p": 9092,
             *     "resp_pkts": 0,
             *     "resp_ip_bytes": 0,
             *     "local_orig": true,
             *     "orig_ip_bytes": 0,
             *     "orig_pkts": 0,
             *     "missed_bytes": 0,
             *     "history": "Cc",
             *     "tunnel_parents": [],
             *     "id.orig_p": 56762,
             *     "local_resp": true,
             *     "uid": "Ct3Ms01I3Yc6pmMZx7",
             *     "conn_state": "OTH",
             *     "id.orig_h": "172.17.0.2",
             *     "proto": "tcp",
             *     "id.resp_h": "172.17.0.3",
             *     "ts": 1487596886.953917
             *   }
             * }
             * 
             * The "conn" first level field states that the event if of type connection.
             */
            String recordValue = (String)record.getField(FieldDictionary.RECORD_VALUE).getRawValue();
            
            // Parse as JSON object
            Map<String, Object> jsonBroEvent = JsonUtil.convertJsonToMap(recordValue);

            if (jsonBroEvent.isEmpty())
            {
                logger.error("Empty Bro event or error while parsing it: " + record);
                continue;
            }
            
            if (jsonBroEvent.size() != 1)
            {
                logger.error("Bro event should have one bro event type field: " + record);
                continue;
            }
            
            Map.Entry<String, Object> eventTypeAndValue = jsonBroEvent.entrySet().iterator().next();
            
            String broEventType = eventTypeAndValue.getKey();
            Object broEventValue = eventTypeAndValue.getValue();
            
            Map<String, Object> finalBroEvent = null; 
            try {
                finalBroEvent = (Map<String, Object>)broEventValue;
            } catch(Throwable t)
            {
                logger.error("Cannot understand bro event content: " + record);
                continue;
            }
            
            // If debug is enabled, we keep the original content of the bro event in the record_value field. Otherwise
            // we remove the record_value field.
            if (debug)    
            {
                // Log original JSON string in record_value for debug purpose
                // Clone the map so that even if we change keys in the map, the original key values are kept
                // in the record_value field
                Map<String, Object> normalizedMap = cloneMap(jsonBroEvent);
                normalizeFields(normalizedMap, null); // Must change '.' characters anyway if want to be able to index in ES  
                record.setField(new Field(FieldDictionary.RECORD_KEY, FieldType.STRING, "bro_event_raw"));
                record.setField(new Field(FieldDictionary.RECORD_VALUE, FieldType.MAP, normalizedMap));
            } else
            {
                record.removeField(FieldDictionary.RECORD_KEY);
                record.removeField(FieldDictionary.RECORD_VALUE);
            }
            
            /**
             * Normalize Bro fields and set first level fields of the record.
             * Our previous Bro event example will give the following Logisland record:
             * 
             * "@timestamp": "2017-02-20T13:36:32Z"
             * "record_id": "6361f80a-c5c9-4a16-9045-4bb51736333d"
             * "record_time": 1487597792782
             * "record_type": "conn"
             * "id_resp_p": 9092
             * "resp_pkts": 0
             * "resp_ip_bytes": 0
             * "local_orig": true
             * "orig_ip_bytes": 0
             * "orig_pkts": 0
             * "missed_bytes": 0
             * "history": "Cc"
             * "tunnel_parents": []
             * "id_orig_p": 56762
             * "local_resp": true
             * "uid": "Ct3Ms01I3Yc6pmMZx7"
             * "conn_state": "OTH"
             * "id_orig_h": "172.17.0.2"
             * "proto": "tcp"
             * "id_resp_h": "172.17.0.3"
             * "ts": 1487596886.953917 
             */

            // Normalize the map key values (Some special characters like '.' are not possible when indexing ion ES)
            normalizeFields(finalBroEvent, null);                       
            
            // Set every first level fields of the Bro event as first level fields of the record for easier processing
            // in processors following in the current processors stream.
            setBroEventFieldsAsFirstLevelFields(finalBroEvent, record);

            // Overwrite default record_type field to indicate to ES processor which index type to use
            // (index type is the bro event type)
            record.setStringField(FieldDictionary.RECORD_TYPE, broEventType);
        }

        if (debug)
        {
            logger.debug("Bro Processor records output: " + records);
        }
        return records;
    }
    
    /**
     * Sets the first level fields of the passed Bro event as first level fields in the passed Logisland record.
     * @param broEvent Bro event.
     * @param record Record for which first level fields should be set. 
     */
    private static void setBroEventFieldsAsFirstLevelFields(Map<String, Object> broEvent, Record record)
    {
        for (Map.Entry<String, Object> jsonEntry : broEvent.entrySet())
        {
            String key = jsonEntry.getKey();
            Object value = jsonEntry.getValue();

            // Id this is a version field, bu sure it is a string
            if (key.equals(FIELD_VERSION))
            {
                if (normalizeVersionField(record, value))
                {
                    // Field processed, go to next
                    continue;
                }
            }

            if (value instanceof String)
            {
                record.setStringField(key, value.toString());
            } else if (value instanceof Integer)
            {
                record.setField(new Field(key, FieldType.INT, value));
            } else if (value instanceof Long)
            {
                record.setField(new Field(key, FieldType.LONG, value));
            } else if (value instanceof ArrayList)
            {
                record.setField(new Field(key, FieldType.ARRAY, value));
            } else if (value instanceof Float)
            {
                record.setField(new Field(key, FieldType.FLOAT, value));
            } else if (value instanceof Double)
            {
                /**
                 * Replace "ts": 1508450363.389543, with "ts": 1508450363389 (from double seconds to long milliseconds)
                 * Change double version to long version because elasticsearch dates do only support longs and
                 * we will use this field in dashboards as this is the closest time of the real event as this is the
                 * value for the time set by bro himself.
                 */
                if (key.equals(FIELD_TS)) {
                    double doubleEpochMilliSeconds = (Double)((double)value * (double)1000); // Number of seconds to number of milliseconds
                    Long longEpochMilliSeconds = (long)doubleEpochMilliSeconds;
                    value = longEpochMilliSeconds;
                    record.setField(new Field(key, FieldType.LONG, value));
                } else {
                    record.setField(new Field(key, FieldType.DOUBLE, value));
                }
            } else if (value instanceof Map)
            {
                record.setField(new Field(key, FieldType.MAP, value));
            } else if (value instanceof Boolean)
            {
                record.setField(new Field(key, FieldType.BOOLEAN, value));
            } else
            {
                // Unrecognized value type, use string
                record.setStringField(key, JsonUtil.convertToJson(value));
            }
        }
    }

    /**
     * Set the version field as being always a string. SSH and SSL both have a version field, but one is a number
     * whereas the other one is a string. As we save every events in the same ES index (even if not the same ES type),
     * one cannot have more than one type for a field in the same index, so we need to choose one.
     * As SSL version  may be for instance "TLSv12", we choose to represent the version always with a string as
     * it also support a number representation (i.e "12"). So here we transform the version field into a string
     * even if the input type was a number
     * @param record Record to update
     * @param value Effective value to transform
     * @return true if the field was processed, false otherwise
     */
    private static boolean normalizeVersionField(Record record, Object value)
    {
        if (value instanceof String)
        {
            record.setStringField(FIELD_VERSION, value.toString());
            return true;
        } else if (value instanceof Integer)
        {
            record.setField(new Field(FIELD_VERSION, FieldType.STRING, value.toString()));
            return true;
        } else if (value instanceof Long)
        {
            record.setField(new Field(FIELD_VERSION, FieldType.STRING, value.toString()));
            return true;
        } else if (value instanceof Float)
        {
            record.setField(new Field(FIELD_VERSION, FieldType.STRING, value.toString()));
            return true;
        } else if (value instanceof Double)
        {
            record.setField(new Field(FIELD_VERSION, FieldType.STRING, value.toString()));
            return true;
        }

        return false;
    }
    
    /**
     * Deeply clones the passed map regarding keys (so that one can modify keys of the original map without changing
     * the clone).
     * @param origMap Map to clone.
     * @return Cloned map.
     */
    private static Map<String, Object> cloneMap(Map<String, Object> origMap)
    {
        Map<String, Object> finalMap = new HashMap<String, Object>();
        origMap.forEach( (key, value) -> {
            if (value instanceof Map)
            {
                Map<String, Object> map = (Map<String, Object>)value;
                finalMap.put(key, (Object)cloneMap(map)); 
            } else
            {
                finalMap.put(key, value);
            }
        });
        return finalMap;
    }
    
    /**
     * Normalize keys in the JSON Bro event:
     * - replace any '.' character in the field names with an acceptable character for ES indexing (currently '_' so for
     * instance id.orig_h becomes id_orig_h). This must be done up to the highest depth of the event (event may contain
     * sub maps).
     * @param broEvent Bro event to normalize.
     * @param oldToNewKeys Potential mapping of keys to change into another key (old key -> new key). May be null.
     */
    private static void normalizeFields(Map<String, Object> broEvent, Map<String, String> oldToNewKeys)
    {
        List<String> keys = new ArrayList<String>(); // Do not modify the map while iterating over it
        for (String key : broEvent.keySet())
        {
            keys.add(key);
        }
        for (String key : keys)
        {
            Object value = broEvent.get(key);
            // Is it a key to replace ?
            String newKey = null;
            if ( (oldToNewKeys != null) && oldToNewKeys.containsKey(key) ) // If the oldToNewKeys map is null, do nothing
            {
                newKey = oldToNewKeys.get(key);
            } else
            {
                // Not a special key to replace but we must at least remove unwanted characters
                if (key.contains("."))
                {
                    newKey = key.replaceAll("\\.", "_");
                }
            }            
            
            // Compute new value
            Object newValue = null;
            if (value instanceof Map)
            {
                Map<String, Object> map = (Map<String, Object>)value;
                normalizeFields(map, oldToNewKeys);
                newValue = map;
            } else
            {
                newValue = value;
            }
            
            if (newKey != null)
            {
                broEvent.remove(key);
                broEvent.put(newKey, newValue);
            }
        }
    }
    
    @Override
    public void onPropertyModified(PropertyDescriptor descriptor, String oldValue, String newValue) {

        logger.debug("property {} value changed from {} to {}", descriptor.getName(), oldValue, newValue);
        
        /**
         * Handle the debug property
         */
        if (descriptor.getName().equals(KEY_DEBUG))
        {
          if (newValue != null)
          {
              if (newValue.equalsIgnoreCase("true"))
              {
                  debug = true;
              }
          } else
          {
              debug = false;
          }
        }
    }   
}
