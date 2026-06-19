package com.shopbilling.service.impl;

import com.shopbilling.dto.request.CreditPaymentRequest;
import com.shopbilling.dto.request.CustomerCreditRequest;
import com.shopbilling.dto.response.CreditPaymentResponse;
import com.shopbilling.dto.response.CustomerCreditResponse;
import com.shopbilling.entity.*;
import com.shopbilling.enums.CreditStatus;
import com.shopbilling.exception.BusinessException;
import com.shopbilling.exception.ResourceNotFoundException;
import com.shopbilling.repository.*;
import com.shopbilling.service.CustomerCreditService;
import com.shopbilling.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class CustomerCreditServiceImpl implements CustomerCreditService {

    private final CustomerCreditRepository creditRepository;
    private final CreditPaymentRepository paymentRepository;
    private final InvoiceRepository invoiceRepository;
    private final SecurityUtils securityUtils;

    private static final List<CreditStatus> PENDING_STATUSES =
            Arrays.asList(CreditStatus.PENDING, CreditStatus.PARTIAL);

    @Override
    public CustomerCreditResponse createCredit(CustomerCreditRequest request) {
        User currentUser = securityUtils.getCurrentUser();

        Invoice invoice = invoiceRepository.findById(request.getInvoiceId())
                .orElseThrow(() -> new ResourceNotFoundException("Invoice", "id", request.getInvoiceId()));

        // Prevent duplicate credit for same invoice
        if (creditRepository.findByInvoice_Id(invoice.getId()).isPresent()) {
            throw new BusinessException("A credit record already exists for invoice " + invoice.getInvoiceNumber());
        }

        BigDecimal totalAmount = invoice.getGrandTotal();
        BigDecimal amountPaid = request.getAmountPaid() != null ? request.getAmountPaid() : BigDecimal.ZERO;

        if (amountPaid.compareTo(totalAmount) >= 0) {
            throw new BusinessException("Amount paid is equal to or more than the invoice total — no credit needed.");
        }
        if (amountPaid.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException("Amount paid cannot be negative.");
        }

        BigDecimal outstanding = totalAmount.subtract(amountPaid);

        CustomerCredit credit = CustomerCredit.builder()
                .invoice(invoice)
                .customer(invoice.getCustomer())
                .customerName(invoice.getCustomerName())
                .customerMobile(invoice.getCustomerMobile())
                .totalAmount(totalAmount)
                .amountPaid(amountPaid)
                .outstandingAmount(outstanding)
                .status(amountPaid.compareTo(BigDecimal.ZERO) == 0
                        ? CreditStatus.PENDING : CreditStatus.PARTIAL)
                .notes(request.getNotes())
                .createdBy(currentUser)
                .build();

        credit = creditRepository.save(credit);
        log.info("Credit created for invoice {} — outstanding: {}", invoice.getInvoiceNumber(), outstanding);
        return toResponse(credit);
    }

    @Override
    public CustomerCreditResponse recordPayment(Long creditId, CreditPaymentRequest request) {
        User currentUser = securityUtils.getCurrentUser();

        CustomerCredit credit = creditRepository.findById(creditId)
                .orElseThrow(() -> new ResourceNotFoundException("Credit", "id", creditId));

        if (credit.getStatus() == CreditStatus.CLEARED) {
            throw new BusinessException("This credit is already fully cleared.");
        }

        if (request.getAmount().compareTo(credit.getOutstandingAmount()) > 0) {
            throw new BusinessException(
                    "Payment amount (" + request.getAmount() + ") exceeds outstanding balance ("
                            + credit.getOutstandingAmount() + ").");
        }

        // Record the payment
        CreditPayment payment = CreditPayment.builder()
                .credit(credit)
                .amount(request.getAmount())
                .paymentMode(request.getPaymentMode())
                .notes(request.getNotes())
                .recordedBy(currentUser)
                .build();
        paymentRepository.save(payment);

        // Update credit balances
        BigDecimal newAmountPaid = credit.getAmountPaid().add(request.getAmount());
        BigDecimal newOutstanding = credit.getTotalAmount().subtract(newAmountPaid);

        credit.setAmountPaid(newAmountPaid);
        credit.setOutstandingAmount(newOutstanding);
        credit.setStatus(newOutstanding.compareTo(BigDecimal.ZERO) == 0
                ? CreditStatus.CLEARED : CreditStatus.PARTIAL);

        credit = creditRepository.save(credit);
        log.info("Payment of {} recorded for credit {} — new outstanding: {}",
                request.getAmount(), creditId, newOutstanding);
        return toResponse(credit);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CustomerCreditResponse> getAllCredits() {
        return creditRepository.findAllByOrderByCreatedAtDesc()
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<CustomerCreditResponse> getPendingCredits() {
        return creditRepository.findByStatusInOrderByCreatedAtDesc(PENDING_STATUSES)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<CustomerCreditResponse> getCreditsByCustomer(Long customerId) {
        return creditRepository.findByCustomer_IdOrderByCreatedAtDesc(customerId)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public CustomerCreditResponse getCreditByInvoice(Long invoiceId) {
        return creditRepository.findByInvoice_Id(invoiceId)
                .map(this::toResponse)
                .orElse(null);
    }

    @Override
    @Transactional(readOnly = true)
    public long countPendingCredits() {
        return creditRepository.countByStatusIn(PENDING_STATUSES);
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal totalOutstandingAmount() {
        BigDecimal total = creditRepository.sumTotalOutstanding();
        return total != null ? total : BigDecimal.ZERO;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean customerHasOutstandingCredit(Long customerId) {
        return creditRepository.countPendingByCustomerId(customerId) > 0;
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal getCustomerOutstandingAmount(Long customerId) {
        BigDecimal amount = creditRepository.sumOutstandingByCustomerId(customerId);
        return amount != null ? amount : BigDecimal.ZERO;
    }

    private CustomerCreditResponse toResponse(CustomerCredit c) {
        CustomerCreditResponse res = new CustomerCreditResponse();
        res.setId(c.getId());
        res.setInvoiceId(c.getInvoice().getId());
        res.setInvoiceNumber(c.getInvoice().getInvoiceNumber());
        res.setCustomerId(c.getCustomer() != null ? c.getCustomer().getId() : null);
        res.setCustomerName(c.getCustomerName());
        res.setCustomerMobile(c.getCustomerMobile());
        res.setTotalAmount(c.getTotalAmount());
        res.setAmountPaid(c.getAmountPaid());
        res.setOutstandingAmount(c.getOutstandingAmount());
        res.setStatus(c.getStatus());
        res.setNotes(c.getNotes());
        res.setCreatedByUsername(c.getCreatedBy() != null ? c.getCreatedBy().getUsername() : null);
        res.setCreatedAt(c.getCreatedAt());
        res.setUpdatedAt(c.getUpdatedAt());
        res.setPayments(c.getPayments().stream().map(this::toPaymentResponse).collect(Collectors.toList()));
        return res;
    }

    private CreditPaymentResponse toPaymentResponse(CreditPayment p) {
        CreditPaymentResponse res = new CreditPaymentResponse();
        res.setId(p.getId());
        res.setAmount(p.getAmount());
        res.setPaymentMode(p.getPaymentMode());
        res.setNotes(p.getNotes());
        res.setRecordedByUsername(p.getRecordedBy() != null ? p.getRecordedBy().getUsername() : null);
        res.setCreatedAt(p.getCreatedAt());
        return res;
    }
}