package ca.sheridancollege.kumar319.controllers;

import ca.sheridancollege.kumar319.util.AppConstants;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.ModelAndView;

@ControllerAdvice
public class GlobalExceptionHandler extends BaseController {

    @ExceptionHandler({Throwable.class})
    public ModelAndView handleSqlException(Throwable e) {
        ModelAndView modelAndView = new ModelAndView(AppConstants.ERROR);

        Throwable throwable = e;

        while (throwable.getCause() != null) {
            throwable = throwable.getCause();
        }

        modelAndView.addObject(AppConstants.MESSAGE, throwable.getMessage());

        return modelAndView;
    }
}
