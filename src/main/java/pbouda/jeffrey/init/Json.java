/*
 * Jeffrey
 * Copyright (C) 2024 Petr Bouda
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package pbouda.jeffrey.init;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ext.NioPathDeserializer;
import com.fasterxml.jackson.databind.ext.NioPathSerializer;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.nio.file.Path;
import java.util.ArrayList;

public abstract class Json {

    private static final TypeReference<ArrayList<String>> STRING_LIST_TYPE =
            new TypeReference<ArrayList<String>>() {
            };

    private static final SimpleModule CUSTOM_PATH_SERDE = new SimpleModule("PathSerde")
            .addSerializer(Path.class, new NioPathSerializer())
            .addDeserializer(Path.class, new NioPathDeserializer());

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .registerModule(CUSTOM_PATH_SERDE)
            .registerModule(new JavaTimeModule());

    public static JsonNode toTree(Object content) {
        return MAPPER.valueToTree(content);
    }

    public static String toPrettyString(Object obj) {
        try {
            return MAPPER
                    .writerWithDefaultPrettyPrinter()
                    .writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public static ObjectNode createObject() {
        return MAPPER.createObjectNode();
    }

    public static ArrayNode createArray() {
        return MAPPER.createArrayNode();
    }
}
