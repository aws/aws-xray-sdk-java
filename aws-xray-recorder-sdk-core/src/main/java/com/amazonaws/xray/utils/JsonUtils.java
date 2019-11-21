package com.amazonaws.xray.utils;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Utility class to parse JSON documents read in from the file system.
 */
public class JsonUtils {
    private static final JsonFactory jsonFactory = new JsonFactory();

    /**
     * Parses given file for an array field and returns that array as a JSON node.
     * @param filePath - The path to the JSON file to parse.
     * @param fieldName - The name of the field in the JSON document to retrieve. Must be a field pointing to an array.
     * @return A node containing an array object, or null if the field cannot be found.
     * @throws IOException
     */
    public static JsonNode getNodeFromJsonFile(String filePath, String fieldName) throws IOException {
        JsonParser jp = jsonFactory.createParser(new File(filePath));
        jp.setCodec(new ObjectMapper());
        JsonNode jsonNode = jp.readValueAsTree();
        return jsonNode.findValue(fieldName);
    }

    /**
     * Finds all immediate children entries mapped to a given field name in a JSON object.
     * @param rootNode - The node to search for entries. Must be an array node.
     * @param fieldName - The name of the key to search for in rootNode's children.
     * @return A list of values that were mapped to the given field name.
     */
    public static List<String> getMatchingListFromJsonArrayNode(JsonNode rootNode, String fieldName) {
        List<String> retList = new ArrayList<>();
        Iterator<JsonNode> ite = rootNode.elements();

        if (ite == null || !ite.hasNext()) {
            return retList;
        }

        ite.forEachRemaining(field -> {
            JsonNode stringNode = field.get(fieldName);

            // Check if fieldName is present and a valid string
            if (stringNode != null && stringNode.textValue() != "") {
                retList.add(stringNode.textValue());
            }
        });

        return retList;
    }
}
