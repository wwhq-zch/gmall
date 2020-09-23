package com.atguigu.gmall.common.handler;

import com.atguigu.gmall.common.bean.ResponseVo;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * 统一异常处理类
 */
@ControllerAdvice// 面向切面编程（AOP）的体现
public class GlobalExceptionHandler {

    // 常用，自定义异常
    @ExceptionHandler(GmallException.class)
    @ResponseBody
    public ResponseVo error(GmallException e){
        e.printStackTrace();
        return ResponseVo.fail(e.getMsg());
    }

}
