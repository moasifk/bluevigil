
package com.bluevigil.model;

import java.util.ArrayList;
import java.util.List;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import org.apache.commons.lang.builder.ToStringBuilder;

public class LogFileConfig {

    @SerializedName("LogFileName")
    @Expose
    private String logFileName;
    @SerializedName("HbaseTable")
    @Expose
    private String hbaseTable;
    @SerializedName("SourceTopic")
    @Expose
    private String sourceTopic;
    @SerializedName("DestinationTopic")
    @Expose
    private String destinationTopic;
    @SerializedName("RowKeyFields")
    @Expose
    private List<RowKeyField> rowKeyFields = new ArrayList<RowKeyField>();
    @SerializedName("FieldMapping")
    @Expose
    private List<FieldMapping> fieldMapping = new ArrayList<FieldMapping>();

    public String getLogFileName() {
        return logFileName;
    }

    public void setLogFileName(String logFileName) {
        this.logFileName = logFileName;
    }

    public String getHbaseTable() {
        return hbaseTable;
    }

    public void setHbaseTable(String hbaseTable) {
        this.hbaseTable = hbaseTable;
    }

    public String getSourceTopic() {
        return sourceTopic;
    }

    public void setSourceTopic(String sourceTopic) {
        this.sourceTopic = sourceTopic;
    }

    public String getDestinationTopic() {
        return destinationTopic;
    }

    public void setDestinationTopic(String destinationTopic) {
        this.destinationTopic = destinationTopic;
    }

    public List<RowKeyField> getRowKeyFields() {
        return rowKeyFields;
    }

    public void setRowKeyFields(List<RowKeyField> rowKeyFields) {
        this.rowKeyFields = rowKeyFields;
    }

    public List<FieldMapping> getFieldMapping() {
        return fieldMapping;
    }

    public void setFieldMapping(List<FieldMapping> fieldMapping) {
        this.fieldMapping = fieldMapping;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this).append("logFileName", logFileName).append("hbaseTable", hbaseTable).append("sourceTopic", sourceTopic).append("destinationTopic", destinationTopic).append("rowKeyFields", rowKeyFields).append("fieldMapping", fieldMapping).toString();
    }

}