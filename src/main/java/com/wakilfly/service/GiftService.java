package com.wakilfly.service;

import com.wakilfly.dto.response.*;
import com.wakilfly.exception.BadRequestException;
import com.wakilfly.exception.ResourceNotFoundException;
import com.wakilfly.dto.request.UserWithdrawalRequest;
import com.wakilfly.dto.response.UserCashWithdrawalResponse;
import com.wakilfly.model.*;
import com.wakilfly.repository.*;
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
public class GiftService {

    private final VirtualGiftRepository virtualGiftRepository;
    private final GiftTransactionRepository giftTransactionRepository;
    private final UserWalletRepository userWalletRepository;
    private final CoinPackageRepository coinPackageRepository;
    private final UserRepository userRepository;
    private final LiveStreamRepository liveStreamRepository;
    private final UserCashWithdrawalRepository userCashWithdrawalRepository;

    private static final BigDecimal MIN_WITHDRAWAL = new BigDecimal("1000");

    // ============================================
    // COIN PACKAGES
    // ============================================

    /**
     * Get available coin packages
     */
    public List<CoinPackageResponse> getCoinPackages() {
        return coinPackageRepository.findByIsActiveTrueOrderBySortOrderAsc().stream()
                .map(this::mapToCoinPackageResponse)
                .collect(Collectors.toList());
    }

    /**
     * Purchase coins (after payment confirmed)
     */
    @Transactional
    public WalletResponse purchaseCoins(UUID userId, UUID packageId) {
        CoinPackage coinPackage = coinPackageRepository.findById(packageId)
                .orElseThrow(() -> new ResourceNotFoundException("Coin package not found"));

        UserWallet wallet = getOrCreateWallet(userId);
        int totalCoins = coinPackage.getCoinAmount() + coinPackage.getBonusCoins();
        wallet.addCoins(totalCoins);
        wallet = userWalletRepository.save(wallet);

        log.info("User {} purchased {} coins (+{} bonus) for {} TZS",
                userId, coinPackage.getCoinAmount(), coinPackage.getBonusCoins(), coinPackage.getPrice());

        return mapToWalletResponse(wallet);
    }

    // ============================================
    // VIRTUAL GIFTS
    // ============================================

    /**
     * Get available gifts
     */
    public List<VirtualGiftResponse> getAvailableGifts() {
        return virtualGiftRepository.findByIsActiveTrueOrderBySortOrderAsc().stream()
                .map(this::mapToVirtualGiftResponse)
                .collect(Collectors.toList());
    }

    /**
     * Send a gift during live stream
     */
    @Transactional
    public void sendGift(UUID senderId, UUID receiverId, UUID giftId, UUID liveStreamId, int quantity, String message) {
        User sender = userRepository.findById(senderId)
                .orElseThrow(() -> new ResourceNotFoundException("Sender not found"));
        User receiver = userRepository.findById(receiverId)
                .orElseThrow(() -> new ResourceNotFoundException("Receiver not found"));
        VirtualGift gift = virtualGiftRepository.findById(giftId)
                .orElseThrow(() -> new ResourceNotFoundException("Gift not found"));

        UserWallet senderWallet = getOrCreateWallet(senderId);
        int totalCoinCost = gift.getCoinValue() * quantity;

        if (!senderWallet.spendCoins(totalCoinCost)) {
            throw new BadRequestException("Insufficient coins. You need " + totalCoinCost + " coins.");
        }

        BigDecimal totalValue = gift.getPrice().multiply(BigDecimal.valueOf(quantity));

        // Create transaction
        GiftTransaction transaction = GiftTransaction.builder()
                .sender(sender)
                .receiver(receiver)
                .gift(gift)
                .quantity(quantity)
                .totalValue(totalValue)
                .message(message)
                .build();

        // Link to live stream if applicable
        if (liveStreamId != null) {
            LiveStream liveStream = liveStreamRepository.findById(liveStreamId).orElse(null);
            if (liveStream != null) {
                transaction.setLiveStream(liveStream);
                liveStream.addGiftValue(totalValue);
                liveStreamRepository.save(liveStream);
            }
        }

        giftTransactionRepository.save(transaction);
        userWalletRepository.save(senderWallet);

        // Credit receiver's wallet (70% of value - platform takes 30%)
        UserWallet receiverWallet = getOrCreateWallet(receiverId);
        BigDecimal receiverShare = totalValue.multiply(BigDecimal.valueOf(0.70));
        receiverWallet.addCash(receiverShare);
        userWalletRepository.save(receiverWallet);

        log.info("Gift sent: {} x{} from {} to {}, value: {} TZS",
                gift.getName(), quantity, sender.getName(), receiver.getName(), totalValue);
    }

    // ============================================
    // WALLET
    // ============================================

    /**
     * Request withdrawal of gift cash to pesa (host convert balance to mobile money etc.)
     * Deducts from balance when request is created; on reject admin adds back.
     */
    @Transactional
    public UserCashWithdrawalResponse requestCashWithdrawal(UUID userId, UserWithdrawalRequest request) {
        UserWallet wallet = getOrCreateWallet(userId);
        if (request.getAmount().compareTo(MIN_WITHDRAWAL) < 0) {
            throw new BadRequestException("Minimum withdrawal is " + MIN_WITHDRAWAL + " TZS");
        }
        if (wallet.getCashBalance().compareTo(request.getAmount()) < 0) {
            throw new BadRequestException("Insufficient cash balance. You have " + wallet.getCashBalance() + " TZS.");
        }
        if (userCashWithdrawalRepository.existsByUserIdAndStatus(userId, WithdrawalStatus.PENDING)) {
            throw new BadRequestException("You already have a pending withdrawal. Wait for it to be processed.");
        }
        User user = userRepository.getReferenceById(userId);
        wallet.setCashBalance(wallet.getCashBalance().subtract(request.getAmount()));
        userWalletRepository.save(wallet);

        UserCashWithdrawal w = UserCashWithdrawal.builder()
                .user(user)
                .amount(request.getAmount())
                .paymentMethod(request.getPaymentMethod())
                .paymentPhone(request.getPaymentPhone())
                .paymentName(request.getPaymentName())
                .status(WithdrawalStatus.PENDING)
                .build();
        w = userCashWithdrawalRepository.save(w);
        log.info("User {} requested cash withdrawal {} TZS", userId, request.getAmount());
        return mapToWithdrawalResponse(w);
    }

    /**
     * Admin: list user cash withdrawal requests (e.g. pending only).
     */
    public PagedResponse<UserCashWithdrawalResponse> getPendingUserWithdrawals(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<UserCashWithdrawal> pageResult = userCashWithdrawalRepository.findByStatusOrderByCreatedAtDesc(WithdrawalStatus.PENDING, pageable);
        return PagedResponse.<UserCashWithdrawalResponse>builder()
                .content(pageResult.getContent().stream().map(this::mapToWithdrawalResponse).collect(Collectors.toList()))
                .page(pageResult.getNumber())
                .size(pageResult.getSize())
                .totalElements(pageResult.getTotalElements())
                .totalPages(pageResult.getTotalPages())
                .last(pageResult.isLast())
                .first(pageResult.isFirst())
                .build();
    }

    /**
     * Admin: process user cash withdrawal (approve = already deducted; reject = add back to wallet).
     */
    @Transactional
    public UserCashWithdrawalResponse processUserCashWithdrawal(UUID withdrawalId, boolean approve,
            String transactionId, String rejectionReason) {
        UserCashWithdrawal w = userCashWithdrawalRepository.findById(withdrawalId)
                .orElseThrow(() -> new ResourceNotFoundException("Withdrawal not found"));
        if (w.getStatus() != WithdrawalStatus.PENDING) {
            throw new BadRequestException("Withdrawal already processed");
        }
        if (approve) {
            w.setStatus(WithdrawalStatus.COMPLETED);
            w.setTransactionId(transactionId);
            w.setProcessedAt(LocalDateTime.now());
        } else {
            UserWallet wallet = getOrCreateWallet(w.getUser().getId());
            wallet.setCashBalance(wallet.getCashBalance().add(w.getAmount()));
            userWalletRepository.save(wallet);
            w.setStatus(WithdrawalStatus.REJECTED);
            w.setRejectionReason(rejectionReason);
            w.setProcessedAt(LocalDateTime.now());
        }
        w = userCashWithdrawalRepository.save(w);
        log.info("User cash withdrawal {} {}", withdrawalId, approve ? "approved" : "rejected");
        return mapToWithdrawalResponse(w);
    }

    /**
     * Get my withdrawal history (gift cash → pesa)
     */
    public PagedResponse<UserCashWithdrawalResponse> getMyWithdrawals(UUID userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<UserCashWithdrawal> pageResult = userCashWithdrawalRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
        return PagedResponse.<UserCashWithdrawalResponse>builder()
                .content(pageResult.getContent().stream().map(this::mapToWithdrawalResponse).collect(Collectors.toList()))
                .page(pageResult.getNumber())
                .size(pageResult.getSize())
                .totalElements(pageResult.getTotalElements())
                .totalPages(pageResult.getTotalPages())
                .last(pageResult.isLast())
                .first(pageResult.isFirst())
                .build();
    }

    /**
     * Get user's wallet
     */
    public WalletResponse getWallet(UUID userId) {
        UserWallet wallet = getOrCreateWallet(userId);
        return mapToWalletResponse(wallet);
    }

    /**
     * Get gifts sent during a live stream (viewers see who sent what – coins/gift value inapanda)
     */
    public PagedResponse<GiftTransactionResponse> getGiftsForLive(UUID liveStreamId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<GiftTransaction> pageResult = giftTransactionRepository.findByLiveStreamIdOrderByCreatedAtDesc(liveStreamId, pageable);
        return buildPagedGiftResponse(pageResult);
    }

    /**
     * Get received gifts history
     */
    public PagedResponse<GiftTransactionResponse> getReceivedGifts(UUID userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<GiftTransaction> transactions = giftTransactionRepository
                .findByReceiverIdOrderByCreatedAtDesc(userId, pageable);

        return buildPagedGiftResponse(transactions);
    }

    /**
     * Get sent gifts history
     */
    public PagedResponse<GiftTransactionResponse> getSentGifts(UUID userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<GiftTransaction> transactions = giftTransactionRepository
                .findBySenderIdOrderByCreatedAtDesc(userId, pageable);

        return buildPagedGiftResponse(transactions);
    }

    // ============================================
    // HELPERS
    // ============================================

    private UserWallet getOrCreateWallet(UUID userId) {
        return userWalletRepository.findByUserId(userId)
                .orElseGet(() -> {
                    User user = userRepository.getReferenceById(userId);
                    UserWallet wallet = UserWallet.builder().user(user).build();
                    return userWalletRepository.save(wallet);
                });
    }

    private PagedResponse<GiftTransactionResponse> buildPagedGiftResponse(Page<GiftTransaction> page) {
        return PagedResponse.<GiftTransactionResponse>builder()
                .content(page.getContent().stream()
                        .map(this::mapToGiftTransactionResponse)
                        .collect(Collectors.toList()))
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .last(page.isLast())
                .first(page.isFirst())
                .build();
    }

    private WalletResponse mapToWalletResponse(UserWallet wallet) {
        return WalletResponse.builder()
                .id(wallet.getId())
                .coinBalance(wallet.getCoinBalance())
                .cashBalance(wallet.getCashBalance())
                .totalCoinsPurchased(wallet.getTotalCoinsPurchased())
                .totalCoinsSpent(wallet.getTotalCoinsSpent())
                .totalGiftsReceived(wallet.getTotalGiftsReceived())
                .build();
    }

    private VirtualGiftResponse mapToVirtualGiftResponse(VirtualGift gift) {
        return VirtualGiftResponse.builder()
                .id(gift.getId())
                .name(gift.getName())
                .description(gift.getDescription())
                .iconUrl(gift.getIconUrl())
                .animationUrl(gift.getAnimationUrl())
                .price(gift.getPrice())
                .coinValue(gift.getCoinValue())
                .isPremium(gift.getIsPremium())
                .build();
    }

    private CoinPackageResponse mapToCoinPackageResponse(CoinPackage pkg) {
        return CoinPackageResponse.builder()
                .id(pkg.getId())
                .name(pkg.getName())
                .coinAmount(pkg.getCoinAmount())
                .price(pkg.getPrice())
                .bonusCoins(pkg.getBonusCoins())
                .description(pkg.getDescription())
                .iconUrl(pkg.getIconUrl())
                .isPopular(pkg.getIsPopular())
                .build();
    }

    private UserCashWithdrawalResponse mapToWithdrawalResponse(UserCashWithdrawal w) {
        return UserCashWithdrawalResponse.builder()
                .id(w.getId())
                .amount(w.getAmount())
                .paymentMethod(w.getPaymentMethod())
                .paymentPhone(w.getPaymentPhone())
                .paymentName(w.getPaymentName())
                .status(w.getStatus())
                .transactionId(w.getTransactionId())
                .rejectionReason(w.getRejectionReason())
                .processedAt(w.getProcessedAt())
                .createdAt(w.getCreatedAt())
                .build();
    }

    private GiftTransactionResponse mapToGiftTransactionResponse(GiftTransaction tx) {
        return GiftTransactionResponse.builder()
                .id(tx.getId())
                .senderName(tx.getSender().getName())
                .senderPic(tx.getSender().getProfilePic())
                .receiverName(tx.getReceiver().getName())
                .receiverPic(tx.getReceiver().getProfilePic())
                .giftName(tx.getGift().getName())
                .giftIcon(tx.getGift().getIconUrl())
                .quantity(tx.getQuantity())
                .totalValue(tx.getTotalValue())
                .message(tx.getMessage())
                .createdAt(tx.getCreatedAt())
                .build();
    }
}
