package util.excel.comm;

/**
 * 操作类型枚举
 * 
 * @author chenlongjs
 * @date 2016年10月11日 上午11:06:01
 */
public enum OperationType {
    DATE_PARSE(0, "数据转换"), // 数据转换
    DATA_PERSISTENCE(1, "数据持久化"), // 数据持久化
    DATA_CHECK(2, "数据校验"), // 数据校验
    OTHER(3, "其它"); // 其它

    private int code;
    private String desc;

    private OperationType(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public int getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }

}
