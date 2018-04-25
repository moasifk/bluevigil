package com.bluevigil.utils;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.util.Bytes;
import org.apache.log4j.Logger;

import com.bluevigil.model.FieldMapping;
import com.bluevigil.model.LogFileConfig;
import com.bluevigil.model.RowKeyField;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class DynamicJsonParser {
	static Logger LOGGER = Logger.getLogger(DynamicJsonParser.class);
	private static BluevigilProperties props = BluevigilProperties.getInstance();
	private static JsonParser parser = new JsonParser();

	public static Put parseDynamicJson(String line, String NWLOG_FILE_CONFIG_PATH) {
		LOGGER.info("Dynamic json parser " + line);
		parser = new JsonParser();
		Gson gson = new Gson();
		FileReader reader = null;
		try {
			reader = new FileReader(NWLOG_FILE_CONFIG_PATH);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		LogFileConfig mappingData = gson.fromJson(reader, LogFileConfig.class);
		Iterator<FieldMapping> fieldMappingItr = mappingData.getFieldMapping().iterator();
		Iterator<RowKeyField> rowkeyFieldMappingItr = mappingData.getRowKeyFields().iterator();
		// Required fields list from the config file
		Map<Integer, String> backendFieldMap = new HashMap<Integer, String>();
		List<String> rowkeyFieldList = new ArrayList<String>();
		FieldMapping fieldMapping = null;
		RowKeyField rowkeyField = null;

		// Rowkey fields list based on row key field mapping
		while (rowkeyFieldMappingItr.hasNext()) {
			rowkeyField = rowkeyFieldMappingItr.next();
			rowkeyFieldList.add(rowkeyField.getOrder(), rowkeyField.getBackEndField());
		}

		// Backend field mapping list
		while (fieldMappingItr.hasNext()) {
			fieldMapping = fieldMappingItr.next();
			if (fieldMapping.isRequired()) {
				backendFieldMap.put(fieldMapping.getOrder(), fieldMapping.getBackEndField());
			}
		}

		String columnQual = BluevigilConstant.EMPTY_STRING; // Hbase column name
		Map<String, String> parsedJsonMap = parseJsonInputLine(line, backendFieldMap, columnQual,
				new HashMap<String, String>());
		Put put = createHbaseObject(rowkeyFieldList, parsedJsonMap);
		return put;
	}

	/**
	 * Method for parsing the input record to its field name and corresponding
	 * field value
	 * 
	 * @param line
	 *            Input record
	 * @param backendFieldMap
	 *            Backend Field map details from the input config file
	 * @param columnQual
	 *            Qualifier name
	 * @param parsedJsonMap jsonMap containing parsed data key=columnQual, value=jsonElementValue
	 * @return parsedJsonMap
	 */
	public static Map<String, String> parseJsonInputLine(String line, Map<Integer, String> backendFieldMap,
			String columnQual, Map<String, String> parsedJsonMap) {
		JsonObject jsonObject = parser.parse(line).getAsJsonObject();
		Iterator<String> jsonObjectItr = jsonObject.keySet().iterator();
		// Getting the max length of the column qualifier
		int qualMaxLength = Integer.parseInt(props.getProperty("bluevigil.hbase.column.qualifier.maxlength"));
		// Get the connector string for connecting different hbase field name
		String qualConnector = props.getProperty("bluevigil.hbase.column.qualifier.connector");
		final String EMPTY_STRING = BluevigilConstant.EMPTY_STRING;
		while (jsonObjectItr.hasNext()) {
			String jsonObjectKey = jsonObjectItr.next();
			JsonElement jsonElementValue = jsonObject.get(jsonObjectKey);
			jsonObjectKey = Utils.createBluevigilFieldName(jsonObjectKey, qualMaxLength);
			if (jsonElementValue.isJsonObject()) {
				// Create the field name as 3char_3char_3char
				columnQual = getColumnQualifierName(columnQual, qualMaxLength, qualConnector, EMPTY_STRING,
						jsonObjectKey);
				parseJsonInputLine(jsonElementValue.toString(), backendFieldMap, columnQual, parsedJsonMap);
			} else {
				if (backendFieldMap.containsValue(jsonObjectKey)) {
					columnQual = getColumnQualifierName(columnQual, qualMaxLength, qualConnector, EMPTY_STRING,
							jsonObjectKey);
					parsedJsonMap.put(columnQual, jsonElementValue.toString());
					columnQual = EMPTY_STRING;
				}
			}
		}
		return parsedJsonMap;
	}

	private static String getColumnQualifierName(String columnQual, int qualMaxLength, String qualConnector,
			final String EMPTY_STRING, String jsonObjectKey) {
		if (columnQual != EMPTY_STRING) {
			columnQual = columnQual + qualConnector
					+ jsonObjectKey.substring(0, Math.min(jsonObjectKey.length(), qualMaxLength));
		} else {
			columnQual = jsonObjectKey;
		}
		return columnQual;
	}

	/**
	 * @param rowkeyFieldList
	 *            List of row key fields defined during the input file config
	 * @param parsedJsonMap
	 *            Json map containing parsed data for each field
	 * @return put Hbase put object with all column qualifier name and data
	 */
	public static Put createHbaseObject(List<String> rowkeyFieldList, Map<String, String> parsedJsonMap) {
		// Creating row key string
		StringBuffer rowKey = new StringBuffer(BluevigilConstant.EMPTY_STRING);
		for (int i = 0; rowkeyFieldList.size() > i; i++) {
			rowKey.append(parsedJsonMap.get(rowkeyFieldList.get(i)) + "|");
			parsedJsonMap.remove(rowkeyFieldList.get(i));
		}
		Put put = new Put(Bytes.toBytes(rowKey.toString()));

		String columnFamily = props.getProperty("bluevigil.hbase.column.family.name.primary");
		for (Map.Entry<String, String> entry : parsedJsonMap.entrySet()) {
			put.addColumn(Bytes.toBytes(columnFamily), Bytes.toBytes(entry.getKey()), Bytes.toBytes(entry.getValue()));
		}
		return put;
	}
}
