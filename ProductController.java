package ca.sheridancollege.kumar319.controllers;

import ca.sheridancollege.kumar319.dto.binding.ProductAddBindingModel;
import ca.sheridancollege.kumar319.dto.service.CategoryServiceModel;
import ca.sheridancollege.kumar319.dto.service.ProductServiceModel;
import ca.sheridancollege.kumar319.dto.view.CategoryViewModel;
import ca.sheridancollege.kumar319.dto.view.ProductAllViewModel;
import ca.sheridancollege.kumar319.dto.view.ProductDetailsViewModel;
import ca.sheridancollege.kumar319.service.CloudinaryService;
import ca.sheridancollege.kumar319.service.ProductService;
import ca.sheridancollege.kumar319.service.UserService;
import ca.sheridancollege.kumar319.util.AppConstants;
import ca.sheridancollege.kumar319.web.annotations.PageTitle;
import ca.sheridancollege.kumar319.exceptions.ProductNameAlreadyExistsException;
import ca.sheridancollege.kumar319.exceptions.ProductNotFoundException;
import ca.sheridancollege.kumar319.service.CategoryService;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.ModelAndView;

import javax.validation.Valid;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/products")
public class ProductController extends BaseController {

    private final ProductService productService;
    private final UserService userService;
    private final CloudinaryService cloudinaryService;
    private final CategoryService categoryService;
    private final ModelMapper modelMapper;

    @Autowired
    public ProductController(ProductService productService, UserService userService,
                             CloudinaryService cloudinaryService, CategoryService categoryService,
                             ModelMapper modelMapper) {
        this.productService = productService;
        this.userService = userService;
        this.cloudinaryService = cloudinaryService;
        this.categoryService = categoryService;
        this.modelMapper = modelMapper;
    }

    @GetMapping("/add")
    @PreAuthorize("hasRole('ROLE_GUEST')")
    @PageTitle("Add Products")
    public ModelAndView addProduct(@ModelAttribute(name = AppConstants.MODEL) ProductAddBindingModel model,
                                   ModelAndView modelAndView) {
        return loadAndReturnModelAndView(model, modelAndView);
    }

    @PostMapping("/add")
    @PreAuthorize("hasRole('ROLE_GUEST')")
    public ModelAndView addProductConfirm(@Valid @ModelAttribute(name = AppConstants.MODEL) ProductAddBindingModel model,
                                          BindingResult bindingResult,
                                          ModelAndView modelAndView) throws IOException {
        ProductServiceModel productServiceModel = modelMapper.map(model, ProductServiceModel.class);

        if (bindingResult.hasErrors() || model.getImage().isEmpty() ||
                productService.createProduct(productServiceModel, model.getImage()) == null) {

            return loadAndReturnModelAndView(model, modelAndView);
        }
        return redirect("/products/add");
    }

    @GetMapping("/all")
    @PreAuthorize("hasRole('ROLE_GUEST')")
    @PageTitle("Products")
    public ModelAndView allProducts(ModelAndView modelAndView) {

        List<ProductAllViewModel> productAllViewModels =
                mapProductServiceToViewModel(productService.findAllFilteredProducts());

        modelAndView.addObject(AppConstants.PRODUCTS_TO_LOWERCASE, productAllViewModels);

        return view("product/all-products", modelAndView);
    }

    @GetMapping("/details/{id}")
    @PreAuthorize("isAuthenticated()")
    @PageTitle("Product Details")
    public ModelAndView detailsProduct(@PathVariable String id, ModelAndView modelAndView) {
        ProductDetailsViewModel product =
                modelMapper.map(productService.findProductById(id), ProductDetailsViewModel.class);
        modelAndView.addObject(AppConstants.PRODUCT_TO_LOWERCASE, product);

        return view("product/details", modelAndView);
    }

    @GetMapping("/edit/{id}")
    @PreAuthorize("hasRole('ROLE_GUEST')")
    @PageTitle("Edit Product")
    public ModelAndView editProduct(@PathVariable String id, ModelAndView modelAndView,
                                    @ModelAttribute(AppConstants.MODEL) ProductAddBindingModel productAddBindingModel) {

        productAddBindingModel =
                this.modelMapper.map(productService.findProductById(id), ProductAddBindingModel.class);

        List<CategoryViewModel> categoryViewModelList =
                mapCategoryServiceToViewModel(categoryService.findAllFilteredCategories());

        modelAndView.addObject(AppConstants.MODEL, productAddBindingModel);

        modelAndView.addObject(AppConstants.CATEGORIES_TO_LOWER_CASE, categoryViewModelList);

        modelAndView.addObject(AppConstants.PRODUCT_ID, id);

        return view("product/edit-product", modelAndView);
    }


    @PostMapping("/edit/{id}")
    @PreAuthorize("hasRole('ROLE_GUEST')")
    public ModelAndView editProductConfirm(ModelAndView modelAndView, @PathVariable String id,
                                           @Valid @ModelAttribute(AppConstants.MODEL) ProductAddBindingModel model,
                                           BindingResult bindingResult) throws IOException {

        boolean isNewImageUploaded = !model.getImage().isEmpty();

        initImage(model);

        ProductServiceModel productServiceModel = modelMapper.map(model, ProductServiceModel.class);

        productService.editProduct(id, productServiceModel, isNewImageUploaded, model.getImage());

        if (bindingResult.hasErrors() ||
                productService.editProduct(id, productServiceModel, isNewImageUploaded, model.getImage())==null) {
            modelAndView.addObject(AppConstants.CATEGORIES_TO_LOWER_CASE,
                    mapCategoryServiceToViewModel(categoryService.findAllFilteredCategories()));
            modelAndView.addObject(AppConstants.MODEL, model);
            modelAndView.addObject(AppConstants.PRODUCT_ID, id);
            return this.view("product/edit-product", modelAndView);
        }

        return redirect("/products/details/" + id);
    }

    private void initImage(ProductAddBindingModel model) {
        if (model.getImage().isEmpty()){
            MultipartFile multipartFile = new MultipartFile() {
                @Override
                public String getName() {
                    return null;
                }

                @Override
                public String getOriginalFilename() {
                    return null;
                }

                @Override
                public String getContentType() {
                    return null;
                }

                @Override
                public boolean isEmpty() {
                    return false;
                }

                @Override
                public long getSize() {
                    return AppConstants.ZERO_NUMBER;
                }

                @Override
                public byte[] getBytes() throws IOException {
                    return new byte[AppConstants.ZERO_NUMBER];
                }

                @Override
                public InputStream getInputStream() throws IOException {
                    return null;
                }

                @Override
                public void transferTo(File file) throws IOException, IllegalStateException {

                }
            };
            model.setImage(multipartFile);
        }
    }


    @GetMapping("/delete/{id}")
    @PreAuthorize("hasRole('ROLE_GUEST')")
    @PageTitle("Delete Product")
    public ModelAndView deleteProduct(@PathVariable String id, ModelAndView modelAndView,
                                      @ModelAttribute("model") ProductAddBindingModel productAddBindingModel) {

        productAddBindingModel = this.modelMapper.map(productService.findProductById(id), ProductAddBindingModel.class);

        modelAndView.addObject(AppConstants.MODEL, productAddBindingModel);

        modelAndView.addObject(AppConstants.CATEGORIES_TO_LOWER_CASE,
                mapCategoryServiceToViewModel(categoryService.findAllFilteredCategories()));

        modelAndView.addObject(AppConstants.PRODUCT_ID, id);

        return view("product/delete-product", modelAndView);
    }

    @PostMapping("/delete/{id}")
    @PreAuthorize("hasRole('ROLE_GUEST')")
    public ModelAndView deleteProductConfirm(@PathVariable String id) {

        productService.deleteProduct(id);

        return redirect("/products/all");
    }

    @GetMapping("/category/{category}")
    @PreAuthorize("isAuthenticated()")
    public ModelAndView fetchByCategory(@PathVariable String category, ModelAndView modelAndView) {

        List<ProductAllViewModel> products = new ArrayList<>();

        products = category.equals("All")? mapProductServiceToViewModel(productService.findAllFilteredProducts())
                : mapProductServiceToViewModel(productService.findAllByCategoryFilteredProducts(category));

        modelAndView.addObject(AppConstants.CATEGORY_NAME, category);

        modelAndView.addObject(AppConstants.PRODUCTS_TO_LOWERCASE, products);

        return view("product/show-products", modelAndView);
    }

    @GetMapping("/fetch")
    @ResponseBody
    public List<ProductAllViewModel> fetchAllProducts() {

        return mapProductServiceToViewModel(productService.findAllFilteredProducts());

    }

    @GetMapping("/api/find")
    @ResponseBody
    @PreAuthorize("hasRole('ROLE_USER')")
    public List<ProductAllViewModel> searchProducts(@RequestParam(AppConstants.PRODUCT_TO_LOWERCASE) String product) {

        return mapProductServiceToViewModel(productService.findProductsByPartOfName(product));
    }

    private ModelAndView loadAndReturnModelAndView(ProductAddBindingModel productBindingModel,
                                                   ModelAndView modelAndView) {

        List<CategoryViewModel> categories = mapCategoryServiceToViewModel(categoryService.findAllFilteredCategories());

        modelAndView.addObject(AppConstants.MODEL, productBindingModel);

        modelAndView.addObject(AppConstants.CATEGORIES_TO_LOWER_CASE, categories);

        return view("product/add-product", modelAndView);
    }

    private List<ProductAllViewModel> mapProductServiceToViewModel(List<ProductServiceModel> productServiceModels){
        return productServiceModels.stream()
                .map(product -> modelMapper.map(product, ProductAllViewModel.class))
                .collect(Collectors.toList());
    }

    private List<CategoryViewModel> mapCategoryServiceToViewModel(List<CategoryServiceModel> categoryServiceModels){
        return categoryServiceModels.stream()
                .map(product -> modelMapper.map(product, CategoryViewModel.class))
                .collect(Collectors.toList());
    }

    @ExceptionHandler({ProductNotFoundException.class})
    public ModelAndView handleProductNotFound(ProductNotFoundException e) {
        ModelAndView modelAndView = new ModelAndView(AppConstants.ERROR);
        modelAndView.addObject(AppConstants.MESSAGE, e.getMessage());
        modelAndView.addObject(AppConstants.STATUS_CODE, e.getStatusCode());

        return modelAndView;
    }

    @ExceptionHandler({ProductNameAlreadyExistsException.class})
    public ModelAndView handleProductNameALreadyExist(ProductNameAlreadyExistsException e) {
        ModelAndView modelAndView = new ModelAndView(AppConstants.ERROR);
        modelAndView.addObject(AppConstants.MESSAGE, e.getMessage());
        modelAndView.addObject(AppConstants.STATUS_CODE, e.getStatusCode());

        return modelAndView;
    }
}