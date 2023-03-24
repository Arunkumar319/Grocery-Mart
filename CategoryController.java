package ca.sheridancollege.kumar319.controllers;

import ca.sheridancollege.kumar319.dto.binding.CategoryAddBindingModel;
import ca.sheridancollege.kumar319.dto.service.CategoryServiceModel;
import ca.sheridancollege.kumar319.dto.view.CategoryViewModel;
import ca.sheridancollege.kumar319.util.AppConstants;
import ca.sheridancollege.kumar319.web.annotations.PageTitle;
import ca.sheridancollege.kumar319.exceptions.CategoryNotFoundException;
import ca.sheridancollege.kumar319.service.CategoryService;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

import javax.validation.Valid;
import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/categories")
public class CategoryController extends BaseController {

    private final CategoryService categoryService;
    private final ModelMapper modelMapper;

    @Autowired
    public CategoryController(CategoryService categoryService, ModelMapper modelMapper) {
        this.categoryService = categoryService;
        this.modelMapper = modelMapper;
    }

    @GetMapping("/add")
    @PageTitle("Add Category")
    @PreAuthorize("hasRole('ROLE_GUEST')")
    public ModelAndView addCategory(@ModelAttribute(name = AppConstants.MODEL) CategoryAddBindingModel categoryAddBindingModel,
                                    ModelAndView modelAndView) {

        return loadAndReturnModelAndView(categoryAddBindingModel, modelAndView);
    }

    @PostMapping("/add")
    @PreAuthorize("hasRole('ROLE_GUEST')")
    public ModelAndView addCategoryConfirm(@Valid @ModelAttribute(name = AppConstants.MODEL) CategoryAddBindingModel model,
                                           BindingResult bindingResult, ModelAndView modelAndView) {

        CategoryServiceModel categoryServiceModel =
                modelMapper.map(model, CategoryServiceModel.class);

        if (bindingResult.hasErrors() ||
                categoryService.addCategory(categoryServiceModel) == null) {

            return loadAndReturnModelAndView(model, modelAndView);
        }
        return redirect("/categories/all");
    }

    @GetMapping("/all")
    @PreAuthorize("hasRole('ROLE_GUEST')")
    @PageTitle(AppConstants.CATEGORIES)
    public ModelAndView allCategories(ModelAndView modelAndView) {

        List<CategoryViewModel> categories =
                mapCategoryServiceToViewModel(categoryService.findAllFilteredCategories());

        modelAndView.addObject(AppConstants.CATEGORIES_TO_LOWER_CASE, categories);

        return view("category/all-categories", modelAndView);
    }

    @GetMapping("/edit/{id}")
    @PreAuthorize("hasRole('ROLE_GUEST')")
    @PageTitle("Edit Category")
    public ModelAndView editCategory(@PathVariable String id, ModelAndView modelAndView) {

        CategoryViewModel categoryViewModel =
                modelMapper.map(categoryService.findCategoryById(id), CategoryViewModel.class);

        modelAndView.addObject(AppConstants.MODEL, categoryViewModel);

        return view("category/edit-category", modelAndView);
    }

    @PostMapping("/edit/{id}")
    @PreAuthorize("hasRole('ROLE_GUEST')")
    public ModelAndView editCategoryConfirm(@PathVariable String id, @Valid @ModelAttribute(name = "model") CategoryAddBindingModel model,
                                            BindingResult bindingResult, ModelAndView modelAndView) {

        CategoryServiceModel categoryServiceModel =
                modelMapper.map(model, CategoryServiceModel.class);

        if (bindingResult.hasErrors() ||
                categoryService.editCategory(id, categoryServiceModel) == null) {

            modelAndView.addObject(AppConstants.MODEL, model);

            return view("category/edit-category", modelAndView);
        }

        return redirect("/categories/all");
    }

    @GetMapping("/delete/{id}")
    @PreAuthorize("hasRole('ROLE_GUEST')")
    @PageTitle("Delete Category")
    public ModelAndView deleteCategory(@PathVariable String id, ModelAndView modelAndView) {

        CategoryViewModel categoryViewModel =
                modelMapper.map(categoryService.findCategoryById(id), CategoryViewModel.class);

        modelAndView.addObject(AppConstants.MODEL, categoryViewModel);

        return view("category/delete-category", modelAndView);
    }

    @PostMapping("/delete/{id}")
    @PreAuthorize("hasRole('ROLE_GUEST')")
    public ModelAndView deleteCategoryConfirm(@PathVariable String id) {

        categoryService.deleteCategory(id);

        return redirect("/categories/all");
    }

    @GetMapping("/fetch")
    /*@PreAuthorize("hasRole('ROLE_MODERATOR')")*/
    @ResponseBody
    public List<CategoryViewModel> fetchCategories() {
        List<CategoryViewModel> categories =
                mapCategoryServiceToViewModel(categoryService.findAllFilteredCategories());

        return categories;
    }

    private List<CategoryViewModel> mapCategoryServiceToViewModel
            (List<CategoryServiceModel> categoryServiceModels){
        return categoryServiceModels.stream()
                .map(product -> modelMapper.map(product, CategoryViewModel.class))
                .collect(Collectors.toList());
    }

    private ModelAndView loadAndReturnModelAndView
            (CategoryAddBindingModel categoryAddBindingModel, ModelAndView modelAndView) {

        modelAndView.addObject(AppConstants.MODEL, categoryAddBindingModel);

        return view("category/add-category", modelAndView);
    }

    @ExceptionHandler({CategoryNotFoundException.class})
    public ModelAndView handleProductNotFound(CategoryNotFoundException e) {
        ModelAndView modelAndView = new ModelAndView(AppConstants.ERROR);
        modelAndView.addObject(AppConstants.MESSAGE, e.getMessage());
        modelAndView.addObject(AppConstants.STATUS_CODE, e.getStatusCode());

        return modelAndView;
    }
}


