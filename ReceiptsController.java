package ca.sheridancollege.kumar319.controllers;

import ca.sheridancollege.kumar319.dto.service.ReceiptServiceModel;
import ca.sheridancollege.kumar319.dto.view.ReceiptViewModel;
import ca.sheridancollege.kumar319.util.AppConstants;
import ca.sheridancollege.kumar319.web.annotations.PageTitle;
import ca.sheridancollege.kumar319.service.ReceiptService;
import org.modelmapper.ModelMapper;

import ca.sheridancollege.kumar319.exceptions.ReceiptNotFoundException;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

import java.security.Principal;
import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/receipts")
public class ReceiptsController extends BaseController {

    private final ReceiptService receiptService;
    private final ModelMapper modelMapper;

    public ReceiptsController (ReceiptService receiptService,
            ModelMapper modelMapper){
        this.receiptService = receiptService;
        this.modelMapper = modelMapper;
    }

    @GetMapping("/all")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @PageTitle(AppConstants.RECEIPTS)
    public ModelAndView getAllReceipts(ModelAndView modelAndView) {

        List<ReceiptViewModel> allReceipts =
                mapReceiptServiceToViewModel(receiptService.findAllReceipts());

        modelAndView.addObject(AppConstants.RECEIPTS_TO_LOWER_CASE, allReceipts);

        return view("receipt/receipts", modelAndView);
    }

    @GetMapping("/all/details/{id}")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @PageTitle(AppConstants.RECEIPTS_DETAILS)
    public ModelAndView allReceiptDetails(@PathVariable String id, ModelAndView modelAndView) {

        ReceiptViewModel receiptViewModel =
                modelMapper.map(receiptService.findReceiptById(id), ReceiptViewModel.class);

        modelAndView.addObject(AppConstants.RECEIPT_TO_LOWER_CASE, receiptViewModel);

        return super.view("receipt/receipt-details", modelAndView);
    }

    @GetMapping("/my")
    @PreAuthorize("isAuthenticated()")
    public ModelAndView getMyOrders(ModelAndView modelAndView, Principal principal) {

        List<ReceiptViewModel> myReceipts =
                mapReceiptServiceToViewModel(receiptService.findAllReceiptsByUsername(principal.getName()));

        modelAndView.addObject(AppConstants.RECEIPTS_TO_LOWER_CASE, myReceipts);

        return view("receipt/receipts", modelAndView);
    }

    @GetMapping("/my/details/{id}")
    @PreAuthorize("isAuthenticated()")
    @PageTitle(AppConstants.RECEIPTS_DETAILS)
    public ModelAndView myOrderDetails(@PathVariable String id, ModelAndView modelAndView) {

        ReceiptServiceModel receipt = receiptService.findReceiptById(id);

        modelAndView.addObject(AppConstants.RECEIPT_TO_LOWER_CASE, modelMapper.map(receipt, ReceiptViewModel.class));

        return super.view("receipt/receipt-details", modelAndView);
    }

    @PostMapping("/create")
    @PreAuthorize("isAuthenticated()")
    public ModelAndView createReceipt(String orderId, Principal principal) {

        receiptService.createReceipt(orderId, principal.getName());

        return super.redirect("/receipts/my");
    }

    private List<ReceiptViewModel> mapReceiptServiceToViewModel
            (List<ReceiptServiceModel> receiptServiceModels){
        return receiptServiceModels.stream()
                .map(product -> modelMapper.map(product, ReceiptViewModel.class))
                .collect(Collectors.toList());
    }

    @ExceptionHandler({ReceiptNotFoundException.class})
    public ModelAndView handleProductNotFound(ReceiptNotFoundException e) {
        ModelAndView modelAndView = new ModelAndView(AppConstants.ERROR);
        modelAndView.addObject(AppConstants.MESSAGE, e.getMessage());
        modelAndView.addObject(AppConstants.STATUS_CODE, e.getStatusCode());

        return modelAndView;
    }

}
