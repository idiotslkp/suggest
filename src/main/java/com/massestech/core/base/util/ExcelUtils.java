package com.massestech.core.base.util;


import org.apache.commons.lang.StringUtils;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Created by yimaozhen on 2016/3/16.
 */
public class ExcelUtils {

    public static List<String[]> readExcel(File file) throws Exception{
        return readExcel(file, 0);
    }

    /**
     * 取Excel所有数据，包含header
     * @return  List<String[]>
     */
    public static List<String[]> readExcel(File file, int sheetAt) throws Exception{
        InputStream in = new FileInputStream(file);
        return readExcel(in, sheetAt);
    }

    public static List<String[]> readExcel(InputStream in, int sheetAt) throws Exception {
        List<String[]> dataList = new ArrayList<String[]>();
        Sheet sheet = getSheet(in, sheetAt);

        int columnNum = 0;
        if(sheet.getRow(0) != null){
            columnNum = sheet.getRow(0).getLastCellNum();
        }
        if(columnNum > 0){
            for(Row row : sheet){
                //如果该行姓名字段为空，则忽略该行
                if(row.getCell(0) == null || "".equals(row.getCell(0).toString().trim())){
                    continue;
                }
                String[] singleRow = new String[columnNum];
                for(int i = 0; i < columnNum; i++){
                    Cell cell = row.getCell(i, Row.CREATE_NULL_AS_BLANK);
                    // 获取cell里面的值
                    singleRow[i] = getRowValue(cell);
                }
                dataList.add(singleRow);
            }
            if(dataList.size() <= 1){
                throw new RuntimeException("你的表格还没有添加数据，请先添加数据");
            }
        }
        return dataList;
    }

    /**
     * 获取sheet
     */
    public static Sheet getSheet(InputStream in, int sheetAt) throws Exception {
        Workbook wb = WorkbookFactory.create(in);
        Sheet sheet = wb.getSheetAt(sheetAt);
        return sheet;
    }

    /**
     * 根据cell获取cell里面的值
     */
    public static String getRowValue(Cell cell) {
        String rowValue = "";
        // 根据单元格类型,将单元格转换为对应的数据
        switch(cell.getCellType()){
            case Cell.CELL_TYPE_BLANK:
                break;
            case Cell.CELL_TYPE_BOOLEAN:
                rowValue = Boolean.toString(cell.getBooleanCellValue());
                break;
            //数值
            case Cell.CELL_TYPE_NUMERIC:
                if(DateUtil.isCellDateFormatted(cell)){
                    rowValue = String.valueOf(cell.getDateCellValue().getTime());
                }else{
                    cell.setCellType(Cell.CELL_TYPE_STRING);
                    String temp = cell.getStringCellValue();
                    //判断是否包含小数点，如果不含小数点，则以字符串读取，如果含小数点，则转换为Double类型的字符串
                    if(temp.indexOf(".")>-1){
                        rowValue = String.valueOf(new Double(temp)).trim();
                    }else{
                        rowValue = temp.trim();
                    }
                }
                break;
            case Cell.CELL_TYPE_STRING:
                rowValue = cell.getStringCellValue().trim();
                if (rowValue.equals("N/A")) {
                    rowValue = null;
                } else if (rowValue.equals("N/T")) {
                    rowValue = null;
                } else if (rowValue.equals("N//A")) {
                    rowValue = null;
                }
                break;
            case Cell.CELL_TYPE_ERROR:
                break;
            case Cell.CELL_TYPE_FORMULA:
                cell.setCellType(Cell.CELL_TYPE_STRING);
                rowValue = cell.getStringCellValue();
                if(rowValue!=null){
                    rowValue = rowValue.replaceAll("#N/A","").trim();
                }
                break;
            default:
                break;
        }
        return rowValue;
    }

    /**
     * 创建excel文档，
     *  list 数据
     * @param keys list中map的key数组集合
     * @param columnNames excel的列名
     * */
    public static Workbook createWorkBook(List<Map<String, Object>> list, String []keys, String columnNames[]) {
        // 创建excel工作簿
        Workbook wb = new HSSFWorkbook();
        // 创建第一个sheet（页），并命名
        Sheet sheet = wb.createSheet(list.get(0).get("sheetName").toString());
        // 手动设置列宽。第一个参数表示要为第几列设；，第二个参数表示列的宽度，n为列高的像素数。
        for(int i=0;i<keys.length;i++){
            sheet.setColumnWidth((short) i, (short) (35.7 * 200));
        }

        // 创建第一行
        Row row = sheet.createRow((short) 0);

        // 创建两种单元格格式
        CellStyle cs = wb.createCellStyle();
        CellStyle cs2 = wb.createCellStyle();

        // 创建两种字体
        Font f = wb.createFont();
        Font f2 = wb.createFont();

        // 创建第一种字体样式（用于列名）
        f.setFontHeightInPoints((short) 10);
        f.setColor(IndexedColors.BLACK.getIndex());
        f.setBoldweight(Font.BOLDWEIGHT_BOLD);

        // 创建第二种字体样式（用于值）
        f2.setFontHeightInPoints((short) 10);
        f2.setColor(IndexedColors.BLACK.getIndex());

        // 设置第一种单元格的样式（用于列名）
        cs.setFont(f);
        cs.setBorderLeft(CellStyle.BORDER_THIN);
        cs.setBorderRight(CellStyle.BORDER_THIN);
        cs.setBorderTop(CellStyle.BORDER_THIN);
        cs.setBorderBottom(CellStyle.BORDER_THIN);
        cs.setAlignment(CellStyle.ALIGN_CENTER);

        // 设置第二种单元格的样式（用于值）
        cs2.setFont(f2);
        cs2.setBorderLeft(CellStyle.BORDER_THIN);
        cs2.setBorderRight(CellStyle.BORDER_THIN);
        cs2.setBorderTop(CellStyle.BORDER_THIN);
        cs2.setBorderBottom(CellStyle.BORDER_THIN);
        cs2.setAlignment(CellStyle.ALIGN_CENTER);
        //设置列名
        for(int i=0;i<columnNames.length;i++){
            Cell cell = row.createCell(i);
            cell.setCellValue(columnNames[i]);
            cell.setCellStyle(cs);
        }
        //设置每行每列的值
        for (short i = 1; i < list.size(); i++) {
            // Row 行,Cell 方格 , Row 和 Cell 都是从0开始计数的
            // 创建一行，在页sheet上
            Row row1 = sheet.createRow((short) i);
            // 在row行上创建一个方格
            for(short j=0;j<keys.length;j++){
                Cell cell = row1.createCell(j);
                cell.setCellValue(list.get(i).get(keys[j]) == null?" ": list.get(i).get(keys[j]).toString());
                cell.setCellStyle(cs2);
            }
        }
        return wb;
    }

    /**
     * 获取excel里面的数据
     */
    public static List<String[]> getDataList(InputStream inputStream, int sheetAt, int jumpNum) {
        List<String[]> dataList = new LinkedList<>();
        try {
            Sheet sheet = ExcelUtils.getSheet(inputStream, sheetAt);
            // 获取列数
            int columnNum = sheet.getRow(0).getLastCellNum();

            // 遍历sheet,但是跳过前面4行,从第5行开始
            for (int i = jumpNum; i < sheet.getLastRowNum(); i ++) {
                Row row = sheet.getRow(i);
                String[] singleRow = new String[columnNum];

                boolean isNotNullFlag = false; // 定义标识用于判断是否是空的,默认列是空的.
                for(int j = 0; j < columnNum; j++){
                    Cell cell = row.getCell(j, Row.CREATE_NULL_AS_BLANK);
                    // 获取cell里面的值
                    singleRow[j] = ExcelUtils.getRowValue(cell);
                    // 如果不为空那么就改变标识
                    if (StringUtils.isNotBlank(singleRow[j])) {
                        isNotNullFlag = true;
                    }
                }
                // 如果列有数据,那么就将其放到数组里面去.
                if (isNotNullFlag) {
                    dataList.add(singleRow);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return dataList;
    }

}
