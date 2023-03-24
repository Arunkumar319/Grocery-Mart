package ca.sheridancollege.kumar319.controllers;

import ca.sheridancollege.kumar319.util.AppConstants;
import org.springframework.web.servlet.ModelAndView;


public abstract class BaseController {
    protected ModelAndView view(String view, ModelAndView modelAndView) {
        modelAndView.setViewName(view);

        return modelAndView;
    }

    protected ModelAndView view(String view) {
        return this.view(view, new ModelAndView());
    }

    protected ModelAndView redirect(String url){
        return this.view(AppConstants.REDIRECT_BASE_CONTROLLER + url);
    }
}
