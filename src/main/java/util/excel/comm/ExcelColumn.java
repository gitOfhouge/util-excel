package util.excel.comm;

public class ExcelColumn {
    /**
     * 字段名称
     */
    private String fieldName;

    /**
     * 字段显示名称
     */
    private String fieldDispName;

    /**
     * 字段类型
     */
    private int type;

    public ExcelColumn() {

    }

    public ExcelColumn(String fieldName) {
        super();
        this.fieldName = fieldName;
    }

    public ExcelColumn(String fieldName, String fieldDispName) {
        super();
        this.fieldName = fieldName;
        this.fieldDispName = fieldDispName;
    }

    public ExcelColumn(String fieldName, String fieldDispName, int type) {
        super();
        this.fieldName = fieldName;
        this.fieldDispName = fieldDispName;
        this.type = type;
    }

    public String getFieldName() {
        return fieldName;
    }

    public void setFieldName(String fieldName) {
        this.fieldName = fieldName;
    }

    public String getFieldDispName() {
        return fieldDispName;
    }

    public void setFieldDispName(String fieldDispName) {
        this.fieldDispName = fieldDispName;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    @Override
    public String toString() {
        return "ExcelColumn [fieldDispName=" + fieldDispName + ", fieldName=" + fieldName + ", type=" + type + "]";
    }
}
