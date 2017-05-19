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
import java.io.StringWriter;
import java.time.LocalDate;
import java.time.Month;
import java.time.format.DateTimeFormatter;
import javax.json.Json;
import javax.json.JsonBuilderFactory;
import javax.json.stream.JsonGenerator;
import javax.json.stream.JsonParser;
import static javax.json.stream.JsonParser.Event.END_ARRAY;
import static javax.json.stream.JsonParser.Event.END_OBJECT;
import static javax.json.stream.JsonParser.Event.KEY_NAME;
import static javax.json.stream.JsonParser.Event.START_ARRAY;
import static javax.json.stream.JsonParser.Event.START_OBJECT;
import static javax.json.stream.JsonParser.Event.VALUE_STRING;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 *
 * These is the place where the JSON-P Streaming API is tested bacically
 * It consists into 2 main classes: JsonGenerator and JSonParser.
 *
 * @author jefrajames
 */
public class TestStreamingApi {

    private static JsonBuilderFactory factory;

    @BeforeClass
    public static void beforeClass() {
        // Null config parameter is not very elegant!
        // According to the spec, all the methods in this class are safe for use by multiple concurrent threads
        factory = Json.createBuilderFactory(null);
    }

    private String buildEmptyArray() {

        // A character stream that collects its output in a string buffer, which can then be used to construct a string.
        StringWriter sw = new StringWriter();

        // JsonGenerator: writes JSON data to an output source (Writer: character stream, OuutputStream: byte stream) in a streaming way
        JsonGenerator jg = Json.createGenerator(sw);

        // Generates an empty array
        jg.writeStartArray().writeEnd().close();

        return sw.toString();
    }

    @Test
    public void testBuildEmptyJsonArray() {
        assertEquals(buildEmptyArray(), "[]");
    }

    /**
     * JsonParser parses JSON using the pull parsing programming model
     */
    @Test
    public void testParseEmptyArray() {
        StringReader reader = new StringReader(buildEmptyArray());
        JsonParser parser = Json.createParser(reader);

        JsonParser.Event firstEvent = parser.next();
        assertEquals(firstEvent, START_ARRAY);

        JsonParser.Event lastEvent = parser.next();
        assertEquals(lastEvent, END_ARRAY);
    }

    private String buildArray() {

        // A character stream that collects its output in a string buffer, which can then be used to construct a string.
        StringWriter sw = new StringWriter();

        // JsonGenerator: writes JSON data to an output source (Writer: character stream, OuutputStream: byte stream) in a streaming way
        JsonGenerator jg = Json.createGenerator(sw);

        jg.writeStartArray()
                .writeStartObject()
                .write("name", "Vinc")
                .write("birthDate", LocalDate.of(1988, Month.NOVEMBER, 20).format(DateTimeFormatter.ISO_LOCAL_DATE))
                .writeEnd()
                .writeStartObject()
                .write("name", "Paul")
                .write("birthDate", LocalDate.of(1991, Month.SEPTEMBER, 4).format(DateTimeFormatter.ISO_LOCAL_DATE))
                .writeEnd()
                .writeStartObject()
                .write("name", "Alice")
                .write("birthDate", LocalDate.of(1995, Month.SEPTEMBER, 8).format(DateTimeFormatter.ISO_LOCAL_DATE))
                .writeEnd()
                .writeEnd()
                .close();

        return sw.toString();

    }

    @Test
    public void testBuildArray() {
        String ja = buildArray();

        assertNotNull(ja);
        assertTrue(ja.startsWith("[") && ja.endsWith("]"));

    }

    @Test
    public void testParseArray() {

        String ja = buildArray();
        assertNotNull(ja);
        // System.out.println("array=" + ja);

        StringReader reader = new StringReader(ja);
        JsonParser parser = Json.createParser(reader);

        JsonParser.Event e = parser.next();
        assertEquals(e, START_ARRAY);
        // System.out.println("event=" + e);

        e = parser.next();

        int objectCount = 0;

        while (e != END_ARRAY) {

            assertEquals(e, START_OBJECT);


            // Name parsing
            e = parser.next();
            assertEquals(e, KEY_NAME);
            String keyName = parser.getString();
            assertTrue(keyName.equals("name"));
            e = parser.next();
            assertEquals(e, VALUE_STRING);
            String name = parser.getString();


            // BirthDate parsing
            e = parser.next();
            assertEquals(e, KEY_NAME);
            keyName = parser.getString();
            assertTrue(keyName.equals("birthDate"));
            e = parser.next();
            assertEquals(e, VALUE_STRING);
            String birthDateAsString = parser.getString();
            LocalDate birthDate = LocalDate.parse(birthDateAsString);
            assertTrue(birthDate.isBefore(LocalDate.now()));


            e = parser.next();
            assertEquals(e, END_OBJECT);

            objectCount++;
            assertTrue(objectCount <= 3);

            e = parser.next();
        }

        // END_ARRAY here
        // System.out.println("objectCount=" + objectCount);
        assertEquals(objectCount, 3);
    }

}
