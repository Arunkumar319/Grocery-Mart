package ca.sheridancollege.kumar319.controllers;

import ca.sheridancollege.kumar319.beans.enumeration.Status;
import ca.sheridancollege.kumar319.dto.service.OrderProductServiceModel;
import ca.sheridancollege.kumar319.dto.service.OrderServiceModel;
import ca.sheridancollege.kumar319.dto.view.*;
import ca.sheridancollege.kumar319.service.OrderService;
import ca.sheridancollege.kumar319.util.AppConstants;
import ca.sheridancollege.kumar319.web.annotations.PageTitle;
import ca.sheridancollege.kumar319.exceptions.OrderNotFoundException;
import org.modelmapper.ModelMapper;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

import java.security.Principal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/order")
public class OrdersController extends BaseController {

    private final OrderService orderService;
    private final ModelMapper modelMapper;

    public OrdersController(OrderService orderService, ModelMapper modelMapper){
        this.orderService = orderService;
        this.modelMapper = modelMapper;
    }

    @GetMapping("/all")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @PageTitle(AppConstants.ORDERS)
    public ModelAndView getAllOrders(ModelAndView modelAndView) {

        List<OrderViewModel> viewModels = mapListOrderServiceToViewModel(orderService.findAllOrders());

        modelAndView.addObject(AppConstants.ORDERS_TO_LOWER_CASE, viewModels);

        return view("order/all-orders", modelAndView);
    }

    @GetMapping("/all/details/{id}")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ModelAndView allOrderDetails(@PathVariable String id, ModelAndView modelAndView) {

        OrderDetailsViewModel order = loadOrderDetailsViewModel(id);

        modelAndView.addObject(AppConstants.ORDER_TO_LOWER_CASE, order);

        return view("order/order-products", modelAndView);
    }

    @GetMapping("/my")
    @PreAuthorize("isAuthenticated()")
    @PageTitle(AppConstants.MY_ORDERS)
    public ModelAndView getMyOrders(ModelAndView modelAndView, Principal principal) {
        String customerName = principal.getName();

        List<OrderViewModel> myOrders =
                mapListOrderServiceToViewModel(orderService.findOrdersByCustomer(customerName));

        List<MyOrderViewModel> myPendingOrders =
                mapListOrderServiceToMyViewModel(orderService.findOrdersByCustomerAndStatus(customerName, Status.Pending));

        List<MyOrderViewModel> myShippedOrders =
                mapListOrderServiceToMyViewModel(orderService.findOrdersByCustomerAndStatus(customerName, Status.Shipped));

        List<MyOrderViewModel> myDeliveredOrders =
                mapListOrderServiceToMyViewModel(orderService.findOrdersByCustomerAndStatus(customerName, Status.Delivered));

        modelAndView.addObject(AppConstants.ORDERS_TO_LOWER_CASE, myOrders);
        modelAndView.addObject(AppConstants.MY_PENDING_ORDERS, myPendingOrders);
        modelAndView.addObject(AppConstants.MY_SHIPPED_ORDERS, myShippedOrders);
        modelAndView.addObject(AppConstants.MY_DELIVERED_ORDERS, myDeliveredOrders);

        return view("order/my-orders", modelAndView);
    }

    @GetMapping("/my/details/{id}")
    @PreAuthorize("isAuthenticated()")
    @PageTitle(AppConstants.ORDER_DETAILS)
    public ModelAndView myOrderDetails(@PathVariable String id, ModelAndView modelAndView) {

        OrderDetailsViewModel order = loadOrderDetailsViewModel(id);

        modelAndView.addObject(AppConstants.ORDER_TO_LOWER_CASE, order);

        return view("order/order-details", modelAndView);
    }

    @GetMapping("/change/status/{id}")
    @PreAuthorize("isAuthenticated()")
    public ModelAndView orderChangeStatus(@PathVariable String id) {

        orderService.changeOrderStatus(id);

        return redirect("/order/all");
    }


    @GetMapping("/fetch/{status}")
    @ResponseBody
    public List<OrderViewModel> fetchByCategory(@PathVariable String status) {

        return loadOrdersByStatus(status);

    }

    private List<OrderViewModel> loadOrdersByStatus(String status) {

        Status statusStatus = Status.Pending;

        switch (status){
            case AppConstants.STATUS_ALL:
                return mapListOrderServiceToViewModel(orderService.findAllOrders());
            case AppConstants.STATUS_SHIPPED:
                statusStatus = Status.Shipped;
                break;
            case AppConstants.STATUS_DELIVERED:
                statusStatus = Status.Delivered;
                break;
            case AppConstants.STATUS_ACQUIRED:
                statusStatus = Status.Acquired;
                break;
        }

        return mapListOrderServiceToViewModel(orderService.findOrdersByStatus(statusStatus));
    }

    private OrderDetailsViewModel loadOrderDetailsViewModel(String id) {
        OrderServiceModel orderServiceModel = orderService.findOrderById(id);
        List<OrderProductServiceModel> products = orderServiceModel.getProducts();

        OrderDetailsViewModel order = modelMapper.map(orderServiceModel, OrderDetailsViewModel.class);
        List<ShoppingCartItem> items = new ArrayList<>();

        Map<OrderProductServiceModel, Integer> productItems = new HashMap<>();

        for (OrderProductServiceModel product: products) {
            productItems.putIfAbsent(product, AppConstants.ZERO_NUMBER);
            int quantity = productItems.get(product) + AppConstants.ONE_NUMBER;
            productItems.put(product, quantity);
        }

        for (Map.Entry<OrderProductServiceModel, Integer> productKVP : productItems.entrySet()) {
            ShoppingCartItem shoppingCartItem = new ShoppingCartItem();

            shoppingCartItem.setQuantity(productKVP.getValue());
            OrderProductViewModel orderProductViewModel =
                    modelMapper.map(productKVP.getKey(), OrderProductViewModel.class);
            shoppingCartItem.setProduct(orderProductViewModel);

            items.add(shoppingCartItem);
        }
        order.setItems(items);

        return order;
    }

    private List<OrderViewModel> mapListOrderServiceToViewModel(List<OrderServiceModel> orderServiceModel){
        return orderServiceModel.stream()
                .map(order -> modelMapper.map(order, OrderViewModel.class))
                .collect(Collectors.toList());
    }

    private List<MyOrderViewModel> mapListOrderServiceToMyViewModel(List<OrderServiceModel> orderServiceModel){
        return orderServiceModel.stream()
                .map(order -> modelMapper.map(order, MyOrderViewModel.class))
                .collect(Collectors.toList());
    }

    @ExceptionHandler({OrderNotFoundException.class})
    public ModelAndView handleProductNotFound(OrderNotFoundException e) {
        ModelAndView modelAndView = new ModelAndView(AppConstants.ERROR);
        modelAndView.addObject(AppConstants.MESSAGE, e.getMessage());
        modelAndView.addObject(AppConstants.STATUS_CODE, e.getStatusCode());

        return modelAndView;
    }

}
