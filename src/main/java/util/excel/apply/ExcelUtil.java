package util.excel.apply;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.servlet.http.HttpServletResponse;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.DataFormat;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.RichTextString;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFRichTextString;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.web.multipart.commons.CommonsMultipartFile;

import util.excel.comm.ConvertResult;
import util.excel.comm.ErrorType;
import util.excel.comm.ExcelColumn;
import util.excel.comm.ExcelHead;
import util.excel.comm.OperationType;
import util.excel.comm.TransResult;

/**
 * Excel 工具类
 * @author chenlongjs
 * @date 2016年11月1日 下午8:05:18
 */
public class ExcelUtil {

    private enum CellType {
        title, normal, error
    }

    private static final String MSG_INVALID_CONVERT = "数据类型转换失败";
    private static final String MSG_INVALID_ARGUMENT = "输入参数名不正确";
    private static final String DATEPATTERN = "yyyy-MM-dd HH:mm:ss";

    /**
     * 将Java实体集合转成Excel文档，并返回
     * 
     * @param <T>
     * @param excelColumns
     *            字段集合
     * @param response
     *            响应
     * @param entities
     *            被导出的实体集合
     * @throws Exception
     */
    public static <T> void trans2Excel(List<ExcelColumn> excelColumns, HttpServletResponse response, List<T> entities) throws Exception {
        String fileName = new StringBuffer().append("exdata")
                .append(new SimpleDateFormat("yyyyMMddHHmmss").format(new Date()))
                    .append(".xlsx").toString();
        OutputStream out = response.getOutputStream();// 取得输出流
        response.setHeader("Content-disposition", "attachment;filename=" + fileName);
        response.setContentType("application/vnd.ms-excel"); // 定义输出类型
        try {
            Workbook wb = new XSSFWorkbook();
            Sheet sheet = wb.createSheet(fileName);
            DataFormat dataformat = wb.createDataFormat();
            // 设置表格默认列宽度为20个字节
            sheet.setDefaultColumnWidth(20);
            // 设置表头的样式 创建表头
            CellStyle titleStyle = createCellStyle(wb, dataformat, CellType.title);
            Row titleRow = sheet.createRow(0);
            titleRow.setHeight((short) 450);
            for (int i = 0, length = excelColumns.size(); i < length; i++) {
                Cell cell = titleRow.createCell(i);
                cell.setCellStyle(titleStyle);
                RichTextString text = new XSSFRichTextString(excelColumns.get(i).getFieldDispName());
                cell.setCellValue(text);
            }
            // 设置单元格样式, 生成单元格
            CellStyle cellStyle = createCellStyle(wb, dataformat, CellType.normal);
            int rowIndex = 1;
            Row cellrow = null;
            Cell cell = null;
            for (int i = 0, length = entities.size(); i < length; i++) {
                cellrow = sheet.createRow(rowIndex);
                int cellIndex = 0;
                for (int j = 0, length2 = excelColumns.size(); i < length2; i++) {
                    cell = cellrow.createCell(cellIndex);
                    cell.setCellStyle(cellStyle);
                    cell.setCellType(excelColumns.get(j).getType());
                    String textValue = getFieldValue(excelColumns.get(j).getFieldName(), entities.get(i), DATEPATTERN);
                    cell.setCellValue(textValue);
                    cellIndex++;
                }
                rowIndex++;
            }
            wb.write(out);
        } finally {
            out.close();
        }
    }

    /**
     * 生成上传Excel文件对应的错误文件，在原始上传文件中追加操作和错误日志，保存到服务端，返回文件名供下载
     * @param multiFile
     *            上传文件输入流
     * @param exportPath
     *            生成文件输出目录
     * @param excelHead
     *            上传文件Excel列结构
     * @param entities
     *            带错误日志和操作信息的列表
     * 
     * @return
     * @throws Exception
     */
    public static <T> String generateErrorExcel(CommonsMultipartFile multiFile, String exportPath, ExcelHead excelHead, List<ConvertResult<T>> entities) throws Exception {
        OutputStream out = null;
        Workbook wb = WorkbookFactory.create(copy(multiFile.getInputStream()));
        File file = new File(exportPath);
        if (!file.exists()) {
            file.mkdir();
        }
        String outfileName = new StringBuffer().append("error")
                .append(new SimpleDateFormat("yyyyMMddHHmmss").format(new Date()))
                    .append(wb instanceof XSSFWorkbook ? ".xlsx" : ".xls")
                        .toString();
        String outfilePath = exportPath + File.separator + outfileName;
        Sheet sheet = wb.getSheetAt(0);
        DataFormat dataformat = wb.createDataFormat();
        // 设置表头的样式 创建表头
        Row titleRow = sheet.getRow(excelHead.getStartTitleRow());
        int lastCellNum = titleRow.getLastCellNum();
        CellStyle titleStyle = createCellStyle(wb, dataformat, CellType.title);
        Cell successCell = titleRow.createCell(lastCellNum);
        sheet.setColumnWidth(lastCellNum, 10 * 256);
        successCell.setCellValue("成功");
        successCell.setCellStyle(titleStyle);
        Cell operationCell = titleRow.createCell(lastCellNum + 1);
        sheet.setColumnWidth(lastCellNum + 1, 10 * 256);
        operationCell.setCellValue("操作");
        operationCell.setCellStyle(titleStyle);
        Cell errorMsgCell = titleRow.createCell(lastCellNum + 2);
        sheet.setColumnWidth(lastCellNum + 2, 40 * 256);
        errorMsgCell.setCellValue("错误消息");
        errorMsgCell.setCellStyle(titleStyle);
        // 设置单元格样式, 生成单元格
        CellStyle normalCellStyle = createCellStyle(wb, dataformat, CellType.normal);
        CellStyle errorCellStyle = createCellStyle(wb, dataformat, CellType.error);
        int rowIndex = excelHead.getStartDataRow();
        Row cellrow = null;
        ConvertResult<T> entity = null;
        for (int i = 0, length = entities.size(); i < length; i++, rowIndex++) {
            entity = entities.get(i);
            cellrow = sheet.getRow(rowIndex);
            successCell = cellrow.createCell(lastCellNum);
            operationCell = cellrow.createCell(lastCellNum + 1);
            errorMsgCell = cellrow.createCell(lastCellNum + 2);
            if (entity.getSuccess()) {
                successCell.setCellStyle(normalCellStyle);
                successCell.setCellValue("");
                operationCell.setCellStyle(normalCellStyle);
                operationCell.setCellValue("");
                errorMsgCell.setCellStyle(normalCellStyle);
                errorMsgCell.setCellValue("");
            } else {
                successCell.setCellStyle(errorCellStyle);
                successCell.setCellValue("否");
                operationCell.setCellStyle(errorCellStyle);
                operationCell.setCellValue(entity.getOperation().getDesc());
                errorMsgCell.setCellStyle(errorCellStyle);
                errorMsgCell.setCellValue(entity.getMsg());
            }
            operationCell.setCellType(Cell.CELL_TYPE_STRING);
            successCell.setCellType(Cell.CELL_TYPE_STRING);
            errorMsgCell.setCellType(Cell.CELL_TYPE_STRING);
        }
        try {
            out = new FileOutputStream(new File(outfilePath));
            wb.write(out);
            return outfileName;
        } finally {
            if (out != null) {
                out.flush();
                out.close();
            }
        }
    }

    /**
     * CommonsMultipartFile转成Java对象集合
     * 
     * @param multiFile
     * @param excelHead
     *            Excel表头
     * @param clz
     *            对象class
     * @return
     * @throws Exception
     */
    public static <T> TransResult<T> trans2Object(CommonsMultipartFile multiFile, ExcelHead excelHead, Class<T> clz) throws Exception {
        TransResult<T> result = new TransResult<T>();
        ConvertResult<T> mapRtn = null;
        List<ConvertResult<T>> mapList = new ArrayList<ConvertResult<T>>(200);
        Workbook wb = WorkbookFactory.create(copy(multiFile.getInputStream()));
        Sheet sheet = wb.getSheetAt(0);
        Boolean success = true;
        // 校验Excel文件的列是否合法
        Row columnRow = sheet.getRow(excelHead.getStartTitleRow());
        List<ExcelColumn> colunms = excelHead.getColumns();
        for (int j = excelHead.getStartColumn(), length = excelHead.getColumns().size(); j < length; j++) {
            if (!columnRow.getCell(j).getStringCellValue().equals(colunms.get(j).getFieldDispName())) {
                result.setSuccess(false);
                result.setErrorType(ErrorType.HEAD_ERROR);
                result.setMsg("Excel文件格式[列名]不正确，请检查文件后重新上传");
                result.setEntities(null);
                return result;
            }
        }
        T entity = null;
        for (int i = excelHead.getStartDataRow(), length = sheet.getPhysicalNumberOfRows(); i < length; i++) {
            entity = (T) clz.newInstance();
            Row row = sheet.getRow(i);
            mapRtn = parseExcelRow(row, excelHead, entity, DATEPATTERN);
            if (!mapRtn.getSuccess()) {
                success = false;
            }
            mapList.add(mapRtn);
        }
        result.setEntities(mapList);
        if (!success) {
            result.setSuccess(false);
            result.setErrorType(ErrorType.DATA_ERROR);
            result.setMsg("Excel数据解析失败，请下载错误文件");
        }
        return result;
    }

    /**
     * 下载Excel文件
     * 
     * @param response
     *            HttpServletResponse
     * @param filePath
     *            下载文件名
     * @throws Exception
     */
    public static void downloadExcel(HttpServletResponse response, String filePath) throws Exception {
        File file = new File(filePath);
        response.setHeader("Content-disposition", "attachment;filename=" + new String(file.getName().getBytes("UTF-8"), "ISO8859_1"));
        response.setContentType("application/msexcel");// 定义输出类型
        response.setContentLength((int) file.length());
        byte[] buffer = new byte[4096];// 缓冲区
        BufferedOutputStream output = null;
        BufferedInputStream input = null;
        try {
            output = new BufferedOutputStream(response.getOutputStream());
            input = new BufferedInputStream(new FileInputStream(file));
            int n = -1;
            // 遍历，开始下载
            while ((n = input.read(buffer, 0, 4096)) > -1) {
                output.write(buffer, 0, n);
            }
            output.flush();
            response.flushBuffer();
        } finally {
            if (input != null)
                input.close();
            if (output != null)
                output.close();
        }
    }

    /**
     * 更据传入列信息解析Excel的行到实体类中
     * 
     * @param row
     *            Excel的行
     * @param excelHead
     *            上传文件的Excel列结构
     * @param entity
     *            关联实体类class
     * @param dateFormat
     *            日期格式如：yyyy-MM-dd HH:mm:ss
     * @return
     * @throws Exception
     */
    private static <T> ConvertResult<T> parseExcelRow(Row row, ExcelHead excelHead, T entity, String dateFormat) throws Exception {
        ConvertResult<T> result = new ConvertResult<T>();
        Boolean success = true;
        StringBuffer errorConvertField = new StringBuffer();
        StringBuffer errorArgumentField = new StringBuffer();
        String cellValue = "";
        SimpleDateFormat sdf = new SimpleDateFormat(dateFormat);
        int startColumn = excelHead.getStartColumn();
        List<ExcelColumn> excelColunms = excelHead.getColumns();
        for (int j = startColumn, cellNum = row.getLastCellNum(); j < cellNum; j++) {
            String fieldName = excelColunms.get(j - startColumn).getFieldName();
            String fieldDispName = excelColunms.get(j - startColumn).getFieldDispName();
            cellValue = getCellValue(row.getCell(j));
            try {
                Field field = entity.getClass().getDeclaredField(fieldName);
                String clsType = field.getType().toString();
                field.setAccessible(true);
                if ("class java.lang.String".equals(clsType)) {
                    field.set(entity, cellValue);
                } else if ("class java.lang.Integer".equals(clsType) || "int".equals(clsType)) {
                    field.set(entity, "".equals(cellValue) ? null : Integer.parseInt(cellValue));
                } else if ("class java.lang.Long".equals(clsType)) {
                    field.set(entity, "".equals(cellValue) ? null : Long.parseLong(cellValue));
                } else if ("class java.lang.Boolean".equals(clsType)) {
                    field.set(entity, "".equals(cellValue) ? null : Boolean.parseBoolean(cellValue));
                } else if ("float".equals(clsType)) {
                    field.set(entity, "".equals(cellValue) ? null : Float.parseFloat(cellValue));
                } else if ("class java.lang.Double".equals(clsType)) {
                    field.set(entity, "".equals(cellValue) ? null : Double.parseDouble(cellValue));
                } else if ("class java.math.BigDecimal".equals(clsType)) {
                    field.set(entity, "".equals(cellValue) ? null : new BigDecimal(cellValue));
                } else if ("class java.util.Date".equals(clsType)) {
                    field.set(entity, "".equals(cellValue) ? null : sdf.parse(cellValue));
                } else {
                    field.set(entity, cellValue);
                }
            } catch (NoSuchFieldException ex) {
                success = false;
                errorArgumentField.append("[" + fieldDispName + ":" + cellValue + "]");
            } catch (IllegalArgumentException | ParseException ex) {
                success = false;
                errorConvertField.append("[" + fieldDispName + ":" + cellValue + "]");
            }
        }
        if (!success) {
            if (errorConvertField.length() > 0) {
                errorConvertField.insert(0, MSG_INVALID_CONVERT).append("\n");
            }
            if (errorArgumentField.length() > 0) {
                errorArgumentField.insert(0, MSG_INVALID_ARGUMENT).insert(0, errorConvertField);
            }
            result.setMsg(errorArgumentField.toString());
        }
        result.setSuccess(success);
        result.setEntity(entity);
        result.setOperation(OperationType.DATE_PARSE);
        return result;
    }

    /**
     * 更据单元格的类型获取对应的String值
     * 
     * @param cell
     * @return
     */
    private static String getCellValue(Cell cell) {
        String cellValue = "";
        if (cell == null) {
            return cellValue;
        }
        if (cell.getCellType() == Cell.CELL_TYPE_NUMERIC) {
            if (DateUtil.isCellDateFormatted(cell)) {
                cellValue = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(cell.getDateCellValue());
            } else {
                cellValue = new DecimalFormat("#.##").format(cell.getNumericCellValue());
            }
        } else if (cell.getCellType() == Cell.CELL_TYPE_STRING) {
            cellValue = cell.getRichStringCellValue().getString();
        } else if (cell.getCellType() == Cell.CELL_TYPE_FORMULA) {
            cellValue = cell.getCellFormula();
        } else if (cell.getCellType() == Cell.CELL_TYPE_BLANK) {
            cellValue = " ";
        } else if (cell.getCellType() == Cell.CELL_TYPE_ERROR) {
            cellValue = " ";
        }
        return cellValue;
    }

    /**
     * 更据类型创建单元格
     * 
     * @param wb
     * @param dataformat
     * @param type
     * @return
     */
    private static CellStyle createCellStyle(Workbook wb, DataFormat dataformat, CellType type) {
        CellStyle cellStyle = wb.createCellStyle();
        cellStyle.setDataFormat(dataformat.getFormat("@"));
        cellStyle.setBorderBottom(XSSFCellStyle.BORDER_THIN);
        cellStyle.setBorderLeft(XSSFCellStyle.BORDER_THIN);
        cellStyle.setBorderRight(XSSFCellStyle.BORDER_THIN);
        cellStyle.setBorderTop(XSSFCellStyle.BORDER_THIN);
        cellStyle.setVerticalAlignment(XSSFCellStyle.VERTICAL_CENTER);// 垂直居中
        cellStyle.setAlignment(XSSFCellStyle.ALIGN_CENTER);
        switch (type) {
        case title: {
            cellStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            cellStyle.setFillPattern(XSSFCellStyle.SOLID_FOREGROUND);
            Font titleFont = wb.createFont(); // 创建字体对象
            titleFont.setFontHeightInPoints((short) 11); // 设置字体大小
            titleFont.setBoldweight(Font.BOLDWEIGHT_BOLD);// 设置粗体
            cellStyle.setFont(titleFont);
        }
        case normal: {
            Font font = wb.createFont();
            font.setBoldweight(Font.BOLDWEIGHT_NORMAL);
            font.setFontHeightInPoints((short) 11);
            cellStyle.setFont(font);
            cellStyle.setWrapText(true);
        }
        case error: {
            cellStyle.setFillForegroundColor(IndexedColors.RED.getIndex());
            cellStyle.setFillPattern(XSSFCellStyle.SOLID_FOREGROUND);
            Font font = wb.createFont();
            font.setBoldweight(Font.BOLDWEIGHT_NORMAL);
            font.setFontHeightInPoints((short) 11);
            cellStyle.setFont(font);
            cellStyle.setWrapText(true);
        }
        default:
        }
        return cellStyle;
    }

    /**
     * 更据字段名获取实体对应值
     * @param <T>
     * 
     * @param fieldName
     * @param item
     * @param dateFormat
     * @return
     * @throws Exception
     */
    private static <T> String getFieldValue(String fieldName, T entity, String dateFormat) throws Exception {
        if (dateFormat == null || "".equals(dateFormat)) {
            dateFormat = "yyyy-MM-dd HH:mm:ss";
        }
        Class<? extends Object> clz = entity.getClass();
        String textValue = "";
        Field field = null;
        try{
            field = clz.getDeclaredField(fieldName);
        }catch(NoSuchFieldException e){
            field = clz.getSuperclass().getDeclaredField(fieldName);
        }
        field.setAccessible(true);
        Object value = field.get(entity);
        if (value == null) {
            textValue = "";
        } else if (value instanceof Date) {
            textValue = new SimpleDateFormat(dateFormat).format((Date)value);
        } else {
            textValue = value.toString();
        }
        return textValue;
    }

    /**
     * 复制输入流
     * @param in 输入流
     * @return
     * @throws IOException
     */
    private static InputStream copy(InputStream in) throws IOException {
        byte[] data = new byte[256];
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        int length = -1;
        try{
            while (-1 != (length = in.read(data))) {
                out.write(data, 0, length);
            }
            out.flush();
            return new ByteArrayInputStream(data);
        }finally{
            out.close();
        }
    }

}
