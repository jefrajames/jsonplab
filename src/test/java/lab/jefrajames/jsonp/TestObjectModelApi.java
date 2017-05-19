/*
 * Copyright 2017 jefrajames.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package lab.jefrajames.jsonp;

import java.io.StringReader;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonBuilderFactory;
import javax.json.JsonException;
import javax.json.JsonNumber;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonPatchBuilder;
import javax.json.JsonPointer;
import javax.json.JsonReader;
import javax.json.JsonValue;
import static javax.json.JsonValue.ValueType.NUMBER;
import javax.json.stream.JsonParser;
import javax.json.stream.JsonParser.Event;
import static javax.json.stream.JsonParser.Event.END_ARRAY;
import static javax.json.stream.JsonParser.Event.END_OBJECT;
import static javax.json.stream.JsonParser.Event.KEY_NAME;
import static javax.json.stream.JsonParser.Event.START_ARRAY;
import static javax.json.stream.JsonParser.Event.START_OBJECT;
import static javax.json.stream.JsonParser.Event.VALUE_NUMBER;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * This is the place where the JSON-P Object Model is tested. It consists in 5
 * classes: JsonReader/Writer, JsonPointer/Patch/MergePatch.
 *
 * @author jefrajames
 */
public class TestObjectModelApi {

    private static JsonBuilderFactory factory;

    @BeforeClass
    public static void beforeClass() {
        // Null config parameter is not very elegant!
        // According to the spce, all the methods in this class are safe for use by multiple concurrent threads
        factory = Json.createBuilderFactory(null);
    }

    @Test
    public void testHello() {
        JsonObject jo = Json.createObjectBuilder()
                .add("message-content", "hello")
                .add("datetime", LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME))
                .add("thread", Thread.currentThread().toString()).build();

        // Just a minimalist test to check the JSON serialization
        assertTrue(jo.toString().contains("hello"));
    }

    private JsonObject buildJsonObject() {

        // A builder is needed for each hierarchy level
        // Root object
        JsonObject value = factory.createObjectBuilder()
                .add("firstName", "John")
                .add("lastName", "Smith")
                .add("age", 25)
                // Some dynamic elements
                .add("thread", Thread.currentThread().toString())
                .add("datetime", LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME))
                // Embedded object
                .add("address", factory.createObjectBuilder()
                        .add("streetAddress", "21 2nd Street")
                        .add("city", "New York")
                        .add("state", "NY")
                        .add("postalCode", "10021"))
                // Embedded array
                .add("phoneNumber", factory.createArrayBuilder()
                        .add(factory.createObjectBuilder()
                                .add("type", "home")
                                .add("number", "212 555-1234"))
                        .add(factory.createObjectBuilder()
                                .add("type", "fax")
                                .add("number", "646 555-4567")))
                .build();

        return value;
    }

    @Test
    public void testObjectBuilder() {
        assertNotNull(buildJsonObject());
    }

    /**
     * JsonReader reads a JSON object or an array structure from an input
     * source.
     */
    @Test
    public void testReader() {
        JsonObject original = buildJsonObject();
        try (JsonReader jsonReader = Json.createReader(new StringReader(original.toString()))) {
            JsonObject jo = jsonReader.readObject();
            assertNotNull(jo);
            assertNotNull(jo.getString("thread"));
            assertEquals(original.getString("thread"), jo.getString("thread"));
        }
    }

    // Create an array of JSON objects
    private JsonArray buildJsonArray() {

        JsonArrayBuilder jab = factory.createArrayBuilder();

        for (int i = 0; i < 10; i++) {
            JsonObjectBuilder job = factory.createObjectBuilder();
            job.add("key-" + i, i);
            JsonObject jo = job.build();
            jab.add(jo);
        }

        return jab.build();
    }

    @Test
    public void testParseArray() {

        JsonArray ja = buildJsonArray();
        assertNotNull(ja);

        StringReader reader = new StringReader(ja.toString());
        JsonParser parser = Json.createParser(reader);

        Event e = parser.next();
        assertEquals(e, START_ARRAY);

        e = parser.next();

        int objectCount = 0;

        while (e != END_ARRAY) {

            assertEquals(e, START_OBJECT);

            e = parser.next();
            assertEquals(e, KEY_NAME);
            String keyName = parser.getString();
            assertTrue(keyName.startsWith("key-"));

            e = parser.next();
            assertEquals(e, VALUE_NUMBER);
            int value = parser.getInt();
            assertTrue(objectCount <= 10);

            e = parser.next();
            assertEquals(e, END_OBJECT);

            objectCount++;

            e = parser.next();
        }

        // END_ARRAY here
        assertEquals(objectCount, 10);
    }

    @Test
    public void testPointer() {

        JsonArray ja = buildJsonArray();
        assertNotNull(ja);

        JsonPointer pointer = Json.createPointer("/1/key-1");
        assertNotNull(pointer);

        JsonValue value = pointer.getValue(ja);
        assertNotNull(value);

        assertEquals(value.getValueType(), NUMBER);

        JsonNumber jn = (JsonNumber) value;
        assertTrue(jn.intValue() == 1);

    }

    @Test(expected = JsonException.class)
    public void testPatchRemove() {

        JsonArray ja = buildJsonArray();
        // ja=[{"key-0":0},{"key-1":1},{"key-2":2},{"key-3":3},{"key-4":4},{"key-5":5},{"key-6":6},{"key-7":7},{"key-8":8},{"key-9":9}]
        assertNotNull(ja);

        // Create a patch builder
        JsonPatchBuilder builder = Json.createPatchBuilder();
        assertNotNull(builder);

        // Remove an element
        JsonArray result = builder.remove("/2/key-2").build().apply(ja);
        // result=[{"key-0":0},{"key-1":1},{},{"key-3":3},{"key-4":4},{"key-5":5},{"key-6":6},{"key-7":7},{"key-8":8},{"key-9":9}]

        // Check that the element has been removed
        JsonPointer pointer = Json.createPointer("/2/key-2");
        assertTrue(pointer.containsValue(ja));
        assertFalse(pointer.containsValue(result));

        // JsonException: Non-existing name/value pair in the object for key key-2
        JsonValue value = pointer.getValue(result);
    }

}
