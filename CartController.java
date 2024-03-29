package ca.sheridancollege.kumar319.controllers;

import ca.sheridancollege.kumar319.dto.service.OrderProductServiceModel;
import ca.sheridancollege.kumar319.dto.service.OrderServiceModel;
import ca.sheridancollege.kumar319.dto.view.OrderProductViewModel;
import ca.sheridancollege.kumar319.dto.view.ProductDetailsViewModel;
import ca.sheridancollege.kumar319.dto.view.ShoppingCartItem;
import ca.sheridancollege.kumar319.service.OrderService;
import ca.sheridancollege.kumar319.service.ProductService;
import ca.sheridancollege.kumar319.service.UserService;
import ca.sheridancollege.kumar319.util.AppConstants;
import ca.sheridancollege.kumar319.web.annotations.PageTitle;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpSession;
import java.math.BigDecimal;
import java.security.Principal;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

@Controller
@RequestMapping("/cart")
public class CartController extends BaseController {

    private final ProductService productService;
    private final UserService userService;
    private final OrderService orderService;
    private final ModelMapper modelMapper;

    @Autowired
    public CartController(ProductService productService, UserService userService,
                          OrderService orderService, ModelMapper modelMapper) {
        this.productService = productService;
        this.userService = userService;
        this.orderService = orderService;
        this.modelMapper = modelMapper;
    }


    @PostMapping("/add-product")
    @PreAuthorize("isAuthenticated()")
    public ModelAndView addToCartConfirm(String id, int quantity, HttpSession session) {

        ShoppingCartItem cartItem = initCartItem(id, quantity);

        List<ShoppingCartItem> cart = retrieveCart(session);

        addItemToCart(cartItem, cart);

        return redirect("/home");
    }

    @GetMapping("/details")
    @PreAuthorize("isAuthenticated()")
    @PageTitle("Cart Details")
    public ModelAndView cartDetails(ModelAndView modelAndView, HttpSession session) {

        List<ShoppingCartItem> cart = retrieveCart(session);

        modelAndView.addObject(AppConstants.TOTAL_PRICE, calcTotal(cart));

        return view("cart/cart-details", modelAndView);
    }

    @DeleteMapping("/remove-product")
    @PreAuthorize("isAuthenticated()")
    public ModelAndView removeFromCartConfirm(String id, HttpSession session) {

        removeItemFromCart(id, retrieveCart(session));

        return redirect("/cart/details");
    }

    @PostMapping("/checkout")
    @PreAuthorize("isAuthenticated()")
    public ModelAndView checkoutConfirm(HttpSession session, Principal principal) {
        List<ShoppingCartItem> cart = retrieveCart(session);

        OrderServiceModel orderServiceModel = prepareOrder(cart, principal.getName());
        if (orderServiceModel.getProducts().isEmpty()){
            return redirect("/cart/details");
        }
        orderService.createOrder(orderServiceModel);

        session.setAttribute("shopping-cart", new LinkedList<>());

        return redirect("/home");
    }

    private List<ShoppingCartItem> retrieveCart(HttpSession session) {
        initCart(session);

        return (List<ShoppingCartItem>) session.getAttribute(AppConstants.SHOPPING_CART);
    }

    private void initCart(HttpSession session) {
        if (session.getAttribute(AppConstants.SHOPPING_CART) == null) {
            session.setAttribute(AppConstants.SHOPPING_CART, new LinkedList<>());
        }
    }

    private ShoppingCartItem initCartItem(String id, int quantity) {
        ProductDetailsViewModel product = modelMapper
                .map(productService.findProductById(id), ProductDetailsViewModel.class);

        OrderProductViewModel orderProductViewModel = new OrderProductViewModel();
        orderProductViewModel.setProduct(product);
        orderProductViewModel.setPrice(product.getPrice());

        ShoppingCartItem cartItem = new ShoppingCartItem();
        cartItem.setProduct(orderProductViewModel);
        cartItem.setQuantity(quantity);

        return cartItem;
    }

    private void addItemToCart(ShoppingCartItem item, List<ShoppingCartItem> cart) {
        for (ShoppingCartItem shoppingCartItem : cart) {
            if (shoppingCartItem.getProduct().getProduct().getId().equals(item.getProduct().getProduct().getId())) {
                shoppingCartItem.setQuantity(shoppingCartItem.getQuantity() + item.getQuantity());
                return;
            }
        }

        if (item.getProduct().getProduct().getDiscountedPrice()!= null){
            item.getProduct().setPrice(item.getProduct().getProduct().getDiscountedPrice());
        }

        cart.add(item);
    }

    private void removeItemFromCart(String id, List<ShoppingCartItem> cart) {
        cart.removeIf(ci -> ci.getProduct().getProduct().getId().equals(id));
    }

    private BigDecimal calcTotal(List<ShoppingCartItem> cart) {
        BigDecimal result = new BigDecimal(AppConstants.ZERO_NUMBER);
        for (ShoppingCartItem item : cart) {
            result = result.add(item.getProduct().getPrice().multiply(new BigDecimal(item.getQuantity())));
        }

        return result;
    }

    private OrderServiceModel prepareOrder(List<ShoppingCartItem> cart, String customer) {

        OrderServiceModel orderServiceModel = new OrderServiceModel();
        orderServiceModel.setCustomer(userService.findUserByUserName(customer));
        List<OrderProductServiceModel> products = new ArrayList<>();
        for (ShoppingCartItem item : cart) {
            OrderProductServiceModel productServiceModel =
                    modelMapper.map(item.getProduct(), OrderProductServiceModel.class);

            for (int i = AppConstants.ZERO_NUMBER; i < item.getQuantity(); i++) {
                products.add(productServiceModel);
            }
        }

        orderServiceModel.setProducts(products);
        orderServiceModel.setTotalPrice(calcTotal(cart));

        return orderServiceModel;
    }
}

