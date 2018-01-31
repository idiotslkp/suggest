package com.massestech.core.base;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.massestech.core.base.model.ResponseModel;
import com.massestech.core.base.util.DateUtils;
import lombok.extern.slf4j.Slf4j;
import net.sf.cglib.beans.BeanCopier;
import net.sf.cglib.core.Converter;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.validation.ValidationException;
import java.lang.reflect.Method;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

/**
 * Created by lyb on 2016/10/8.
 */
@Slf4j
public abstract class SimpleController {

//    protected Logger logger = LoggerFactory.getLogger(this.getClass());

    private static final HashMap<String, BeanCopier> beanCopierMap = new HashMap<>();


    protected ResponseEntity success(){
        ResponseModel responseModel = new ResponseModel();
        responseModel.setCode(0);
        responseModel.setMsg("处理成功");

        JSONObject json = JSON.parseObject(JSONObject.toJSONString(responseModel));
        json.remove("data");
        return ResponseEntity.ok().body(json);

    }

    protected <T> ResponseEntity success(T data){
        ResponseModel<T> responseModel = new ResponseModel<>();
        responseModel.setCode(0);
        responseModel.setMsg("处理成功");
        responseModel.setData(data);
        return ResponseEntity.ok().body(responseModel);
    }

    protected ResponseEntity fail() {
        return ResponseEntity.status(200).body(this.fail(-1, "系统繁忙"));
    }

    protected ResponseEntity fail(String msg) {
        return ResponseEntity.status(200).body(this.fail(-1, msg));
    }

    protected ResponseEntity fail(int code, String msg) {
        ResponseModel responseModel = new ResponseModel();
        responseModel.setCode(code);
        responseModel.setMsg(msg);

        JSONObject json = JSON.parseObject(JSONObject.toJSONString(responseModel));
        json.remove("data");

        return ResponseEntity.status(200).body(json);
    }

    protected Pageable pageable(Integer pageNum, Integer pageSize){
        return this.pageable(pageNum, pageSize, null);
    }

    protected Pageable pageable(Integer pageNum, Integer pageSize, Sort sort){
        pageNum = pageNum == null ? 1 : pageNum;
        pageSize = pageSize == null ? 10 : pageSize;
        return new PageRequest(pageNum - 1, pageSize, sort);
    }

    protected void validate(BindingResult result){
        if(result.hasErrors()){
            String errorsStr = "";
            List<ObjectError> errors = result.getAllErrors();
            for(ObjectError error : errors){
                errorsStr += error.getDefaultMessage() + ",";
            }
            errorsStr = errorsStr.substring(0, errorsStr.length() - 1);
            throw new ValidationException(errorsStr);
        }
    }

    /**
     * 统一处理异常
     * @param e
     * @return
     */
    @ExceptionHandler
    @ResponseBody
    public ResponseEntity exceptionHandler(Exception e) {
        String errmsg = null;
        int code = -1;
        if(e instanceof ValidationException) {
//            e.printStackTrace();
            log.info(e.toString());
            errmsg = e.getMessage();
        }else if(e instanceof BindException) {
//            e.printStackTrace();
            log.info(e.toString());
            errmsg = e.getMessage();
        }else if(e instanceof BaseException) {
//            e.printStackTrace();
            log.info(e.toString());
            errmsg = e.getMessage();
            code = ((BaseException) e).getCode();
        }else if(e instanceof Exception) {
//            e.printStackTrace();
            log.info(e.toString());
            errmsg = "系统繁忙";
        }
        return fail(code, errmsg);
    }

    protected static void copyModelProperties(Object model, Object entity){
        BeanCopier copier = getBeanCopier(model.getClass(), entity.getClass());
        copier.copy(model, entity, new ModelBeanPropertyConverter());
    }

    protected static void copyEntityProperties(Object entity, Object model){
        BeanCopier copier = getBeanCopier(entity.getClass(), model.getClass());
        copier.copy(entity, model, new EntityBeanPropertyConverter());
    }

    protected static BeanCopier getBeanCopier(Class source, Class target) {
        String beanCopierKey = generateBeanKey(source, target);
        if (beanCopierMap.containsKey(beanCopierKey)) {
            return beanCopierMap.get(beanCopierKey);
        } else {
            BeanCopier beanCopier = BeanCopier.create(source, target, true);
            beanCopierMap.putIfAbsent(beanCopierKey, beanCopier);//putIfAbsent是jdk1.8新增方法，如果没有就put
        }
        return beanCopierMap.get(beanCopierKey);
    }

    private static String generateBeanKey(Class source, Class target) {
        return source.getName() + "@" + target.getName();
    }

    private static class ModelBeanPropertyConverter implements Converter {
        @Override
        public Object convert(Object value, Class target, Object methodName) {
            if(value == null){
                return value;
            }
            if (target.isEnum()) {
                try {
                    Method valueOfMethod = target.getMethod("valueOf", String.class);
                    return valueOfMethod.invoke(null, value);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            if (target == Date.class) {
                return DateUtils.convertStringToDate((String) value);
            }
            return value;
        }
    }

    private static class EntityBeanPropertyConverter implements Converter {
        @Override
        public Object convert(Object value, Class target, Object methodName) {
            if(value == null){
                return value;
            }
            if (value != null && value.getClass().isEnum()) {
                return ((Enum)value).name();
            }
            if (value instanceof Date) {
                return DateUtils.converDateToString((Date)value);
            }
            return value;
        }
    }
}
