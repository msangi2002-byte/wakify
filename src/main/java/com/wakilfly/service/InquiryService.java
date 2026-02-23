package com.wakilfly.service;

import com.wakilfly.dto.request.CreateInquiryRequest;
import com.wakilfly.dto.request.QuoteInquiryRequest;
import com.wakilfly.dto.response.InquiryResponse;
import com.wakilfly.dto.response.PagedResponse;
import com.wakilfly.exception.BadRequestException;
import com.wakilfly.exception.ResourceNotFoundException;
import com.wakilfly.model.*;
import com.wakilfly.repository.BusinessRepository;
import com.wakilfly.repository.ProductImageRepository;
import com.wakilfly.repository.ProductInquiryRepository;
import com.wakilfly.repository.ProductRepository;
import com.wakilfly.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class InquiryService {

    private final ProductInquiryRepository inquiryRepository;
    private final ProductRepository productRepository;
    private final ProductImageRepository productImageRepository;
    private final BusinessRepository businessRepository;
    private final UserRepository userRepository;

    @Transactional
    public InquiryResponse create(UUID buyerId, CreateInquiryRequest request) {
        User buyer = userRepository.findById(buyerId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", buyerId));
        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", request.getProductId()));
        if (!product.getIsActive()) {
            throw new BadRequestException("Product is not available for inquiry");
        }
        Business business = product.getBusiness();
        if (business == null) {
            throw new BadRequestException("Product has no business");
        }

        ProductInquiry inquiry = ProductInquiry.builder()
                .buyer(buyer)
                .product(product)
                .business(business)
                .message(request.getMessage())
                .quantity(request.getQuantity() != null && request.getQuantity() >= 1 ? request.getQuantity() : 1)
                .status(InquiryStatus.OPEN)
                .build();
        inquiry = inquiryRepository.save(inquiry);
        log.info("Inquiry {} created for product {} by buyer {}", inquiry.getId(), product.getId(), buyerId);
        return mapToResponse(inquiry);
    }

    public PagedResponse<InquiryResponse> getMyInquiries(UUID buyerId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<ProductInquiry> pageResult = inquiryRepository.findByBuyerIdOrderByCreatedAtDesc(buyerId, pageable);
        return PagedResponse.<InquiryResponse>builder()
                .content(pageResult.getContent().stream().map(this::mapToResponse).collect(Collectors.toList()))
                .page(pageResult.getNumber())
                .size(pageResult.getSize())
                .totalElements(pageResult.getTotalElements())
                .totalPages(pageResult.getTotalPages())
                .last(pageResult.isLast())
                .first(pageResult.isFirst())
                .build();
    }

    public PagedResponse<InquiryResponse> getBusinessInquiries(UUID businessId, UUID ownerId, InquiryStatus status, int page, int size) {
        Business business = businessRepository.findById(businessId)
                .orElseThrow(() -> new ResourceNotFoundException("Business", "id", businessId));
        if (!business.getOwner().getId().equals(ownerId)) {
            throw new BadRequestException("Not your business");
        }
        Pageable pageable = PageRequest.of(page, size);
        Page<ProductInquiry> pageResult = status != null
                ? inquiryRepository.findByBusinessIdAndStatusOrderByCreatedAtDesc(businessId, status, pageable)
                : inquiryRepository.findByBusinessId(businessId, pageable);
        return PagedResponse.<InquiryResponse>builder()
                .content(pageResult.getContent().stream().map(this::mapToResponse).collect(Collectors.toList()))
                .page(pageResult.getNumber())
                .size(pageResult.getSize())
                .totalElements(pageResult.getTotalElements())
                .totalPages(pageResult.getTotalPages())
                .last(pageResult.isLast())
                .first(pageResult.isFirst())
                .build();
    }

    @Transactional
    public InquiryResponse quote(UUID inquiryId, UUID sellerUserId, QuoteInquiryRequest request) {
        ProductInquiry inquiry = inquiryRepository.findById(inquiryId)
                .orElseThrow(() -> new ResourceNotFoundException("Inquiry", "id", inquiryId));
        if (!inquiry.getBusiness().getOwner().getId().equals(sellerUserId)) {
            throw new BadRequestException("Not your business inquiry");
        }
        if (inquiry.getStatus() != InquiryStatus.OPEN) {
            throw new BadRequestException("Inquiry is not open for quote");
        }
        inquiry.setSellerReply(request.getSellerReply());
        inquiry.setQuotedPrice(request.getQuotedPrice());
        inquiry.setQuotedDeliveryFee(request.getQuotedDeliveryFee() != null ? request.getQuotedDeliveryFee() : BigDecimal.ZERO);
        inquiry.setStatus(InquiryStatus.QUOTED);
        inquiry.setRespondedAt(LocalDateTime.now());
        inquiry = inquiryRepository.save(inquiry);
        log.info("Inquiry {} quoted by seller {}", inquiryId, sellerUserId);
        return mapToResponse(inquiry);
    }

    @Transactional
    public InquiryResponse accept(UUID inquiryId, UUID buyerId) {
        ProductInquiry inquiry = inquiryRepository.findById(inquiryId)
                .orElseThrow(() -> new ResourceNotFoundException("Inquiry", "id", inquiryId));
        if (!inquiry.getBuyer().getId().equals(buyerId)) {
            throw new BadRequestException("Not your inquiry");
        }
        if (inquiry.getStatus() != InquiryStatus.QUOTED) {
            throw new BadRequestException("Inquiry must be quoted first");
        }
        inquiry.setStatus(InquiryStatus.ACCEPTED);
        inquiry = inquiryRepository.save(inquiry);
        return mapToResponse(inquiry);
    }

    @Transactional
    public InquiryResponse reject(UUID inquiryId, UUID userId, boolean isSeller) {
        ProductInquiry inquiry = inquiryRepository.findById(inquiryId)
                .orElseThrow(() -> new ResourceNotFoundException("Inquiry", "id", inquiryId));
        if (isSeller) {
            if (!inquiry.getBusiness().getOwner().getId().equals(userId)) {
                throw new BadRequestException("Not your business inquiry");
            }
        } else {
            if (!inquiry.getBuyer().getId().equals(userId)) {
                throw new BadRequestException("Not your inquiry");
            }
        }
        inquiry.setStatus(InquiryStatus.REJECTED);
        inquiry = inquiryRepository.save(inquiry);
        return mapToResponse(inquiry);
    }

    public InquiryResponse getById(UUID inquiryId, UUID userId) {
        ProductInquiry inquiry = inquiryRepository.findById(inquiryId)
                .orElseThrow(() -> new ResourceNotFoundException("Inquiry", "id", inquiryId));
        boolean isBuyer = inquiry.getBuyer().getId().equals(userId);
        boolean isSeller = inquiry.getBusiness().getOwner().getId().equals(userId);
        if (!isBuyer && !isSeller) {
            throw new BadRequestException("Not allowed to view this inquiry");
        }
        return mapToResponse(inquiry);
    }

    public void markConvertedToOrder(UUID inquiryId, UUID orderId) {
        ProductInquiry inquiry = inquiryRepository.findById(inquiryId)
                .orElseThrow(() -> new ResourceNotFoundException("Inquiry", "id", inquiryId));
        inquiry.setStatus(InquiryStatus.CONVERTED_TO_ORDER);
        inquiry.setConvertedOrderId(orderId);
        inquiryRepository.save(inquiry);
    }

    private String getProductThumbnail(Product product) {
        if (product.getThumbnail() != null) return product.getThumbnail();
        List<ProductImage> images = productImageRepository.findByProductIdOrderByDisplayOrderAsc(product.getId());
        return images.isEmpty() ? null : images.get(0).getUrl();
    }

    private InquiryResponse mapToResponse(ProductInquiry i) {
        return InquiryResponse.builder()
                .id(i.getId())
                .status(i.getStatus())
                .productId(i.getProduct().getId())
                .productName(i.getProduct().getName())
                .productThumbnail(getProductThumbnail(i.getProduct()))
                .businessId(i.getBusiness().getId())
                .businessName(i.getBusiness().getName())
                .buyerId(i.getBuyer().getId())
                .buyerName(i.getBuyer().getName())
                .message(i.getMessage())
                .quantity(i.getQuantity())
                .sellerReply(i.getSellerReply())
                .quotedPrice(i.getQuotedPrice())
                .quotedDeliveryFee(i.getQuotedDeliveryFee())
                .createdAt(i.getCreatedAt())
                .respondedAt(i.getRespondedAt())
                .convertedOrderId(i.getConvertedOrderId())
                .build();
    }
}
