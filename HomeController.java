package ca.sheridancollege.kumar319.controllers;

import ca.sheridancollege.kumar319.dto.service.CategoryServiceModel;
import ca.sheridancollege.kumar319.dto.view.CategoryViewModel;
import ca.sheridancollege.kumar319.util.AppConstants;
import ca.sheridancollege.kumar319.web.annotations.PageTitle;
import ca.sheridancollege.kumar319.service.CategoryService;
import org.modelmapper.ModelMapper;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.ModelAndView;

import java.security.Principal;
import java.util.List;
import java.util.stream.Collectors;

@Controller
public class HomeController extends BaseController {

    private final CategoryService categoryService;
    private final ModelMapper modelMapper;

    @Autowired
    public HomeController(CategoryService categoryService, ModelMapper modelMapper) {

        this.categoryService = categoryService;
        this.modelMapper = modelMapper;
    }

    @GetMapping("/")
    @PageTitle(AppConstants.INDEX)
    public ModelAndView renderIndexPage(Principal principal, ModelAndView modelAndView) {

        modelAndView.addObject(AppConstants.PRINCIPAL_TO_LOWER_CASE, principal);

        return view("/index", modelAndView);
    }

    @GetMapping("/home")
    @PreAuthorize("isAuthenticated()")
    @PageTitle(AppConstants.HOME)
    public ModelAndView renderHomePage(Principal principal, ModelAndView modelAndView) {
        
        List<CategoryViewModel> categories =
                mapCategoryServiceToViewModel(categoryService.findAllFilteredCategories());
        
        modelAndView.addObject(AppConstants.PRINCIPAL_TO_LOWER_CASE, principal);

        modelAndView.addObject(AppConstants.CATEGORIES_TO_LOWER_CASE, categories);

        return view("/home", modelAndView);
    }

    private List<CategoryViewModel> mapCategoryServiceToViewModel(List<CategoryServiceModel> categoryServiceModels){
        return categoryServiceModels.stream()
                .map(product -> modelMapper.map(product, CategoryViewModel.class))
                .collect(Collectors.toList());
    }
}
