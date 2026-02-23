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
    private final com.wakilfly.repository.SystemConfigRepository systemConfigRepository;

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
     * Send a gift during live stream.
     * Viewer coins are deducted; creator receives diamonds (creator_split_ratio of coin value; platform keeps the rest).
     */
    @Transactional
    public void sendGift(UUID senderId, UUID receiverId, UUID giftId, UUID liveStreamId, int quantity, String message) {
        User sender = userRepository.findById(senderId)
                .orElseThrow(() -> new ResourceNotFoundException("Sender not found"));
        User receiver = userRepository.findById(receiverId)
                .orElseThrow(() -> new ResourceNotFoundException("Receiver not found"));
        VirtualGift gift = virtualGiftRepository.findById(giftId)
                .orElseThrow(() -> new ResourceNotFoundException("Gift not found"));

        int coinValue = gift.getCoinValue() != null ? gift.getCoinValue() : 0;
        if (coinValue <= 0) {
            throw new BadRequestException("Gift has invalid coin value");
        }

        UserWallet senderWallet = getOrCreateWallet(senderId);
        int totalCoinCost = coinValue * quantity;

        if (!senderWallet.spendCoins(totalCoinCost)) {
            throw new BadRequestException("Insufficient coins. You need " + totalCoinCost + " coins.");
        }

        BigDecimal totalValue = gift.getPrice() != null ? gift.getPrice().multiply(BigDecimal.valueOf(quantity)) : BigDecimal.valueOf(totalCoinCost);

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

        // Credit creator with diamonds (platform takes the rest)
        BigDecimal creatorRatio = getConfigDecimal(com.wakilfly.model.SystemConfig.KEY_CREATOR_SPLIT_RATIO, com.wakilfly.model.SystemConfig.DEFAULT_CREATOR_SPLIT_RATIO);
        BigDecimal creatorDiamonds = BigDecimal.valueOf(totalCoinCost).multiply(creatorRatio);
        UserWallet receiverWallet = getOrCreateWallet(receiverId);
        receiverWallet.addDiamonds(creatorDiamonds);
        BigDecimal prev = receiverWallet.getTotalGiftsReceived() != null ? receiverWallet.getTotalGiftsReceived() : BigDecimal.ZERO;
        receiverWallet.setTotalGiftsReceived(prev.add(totalValue));
        userWalletRepository.save(receiverWallet);

        log.info("Gift sent: {} x{} from {} to {}, coins: {}, creator diamonds: {}",
                gift.getName(), quantity, sender.getName(), receiver.getName(), totalCoinCost, creatorDiamonds);
    }

    // ============================================
    // WALLET
    // ============================================

    /**
     * Request withdrawal: convert diamonds to TZS (at rate), deduct diamond balance, create withdrawal request.
     * Min/max TZS from config.
     */
    @Transactional
    public UserCashWithdrawalResponse requestCashWithdrawal(UUID userId, UserWithdrawalRequest request) {
        UserWallet wallet = getOrCreateWallet(userId);
        BigDecimal rate = getConfigDecimal(com.wakilfly.model.SystemConfig.KEY_DIAMOND_TO_TZS_RATE, com.wakilfly.model.SystemConfig.DEFAULT_DIAMOND_TO_TZS_RATE);
        BigDecimal minTzs = getConfigDecimal(com.wakilfly.model.SystemConfig.KEY_WITHDRAWAL_MIN_TZS, com.wakilfly.model.SystemConfig.DEFAULT_WITHDRAWAL_MIN_TZS);
        BigDecimal maxTzs = getConfigDecimal(com.wakilfly.model.SystemConfig.KEY_WITHDRAWAL_MAX_TZS, com.wakilfly.model.SystemConfig.DEFAULT_WITHDRAWAL_MAX_TZS);

        if (request.getAmount().compareTo(minTzs) < 0) {
            throw new BadRequestException("Minimum withdrawal is " + minTzs + " TZS");
        }
        if (request.getAmount().compareTo(maxTzs) > 0) {
            throw new BadRequestException("Maximum withdrawal per request is " + maxTzs + " TZS");
        }

        BigDecimal diamondBal = wallet.getDiamondBalance() != null ? wallet.getDiamondBalance() : BigDecimal.ZERO;
        BigDecimal withdrawableTzs = diamondBal.multiply(rate);
        if (request.getAmount().compareTo(withdrawableTzs) > 0) {
            throw new BadRequestException("Insufficient balance. You have " + withdrawableTzs + " TZS withdrawable (diamonds: " + wallet.getDiamondBalance() + ").");
        }
        if (userCashWithdrawalRepository.existsByUserIdAndStatus(userId, WithdrawalStatus.PENDING)) {
            throw new BadRequestException("You already have a pending withdrawal. Wait for it to be processed.");
        }

        BigDecimal diamondsToDeduct = request.getAmount().divide(rate, 4, java.math.RoundingMode.DOWN);
        wallet.setDiamondBalance(wallet.getDiamondBalance().subtract(diamondsToDeduct));
        userWalletRepository.save(wallet);

        User user = userRepository.getReferenceById(userId);
        UserCashWithdrawal w = UserCashWithdrawal.builder()
                .user(user)
                .amount(request.getAmount())
                .paymentMethod(request.getPaymentMethod())
                .paymentPhone(request.getPaymentPhone())
                .paymentName(request.getPaymentName())
                .status(WithdrawalStatus.PENDING)
                .build();
        w = userCashWithdrawalRepository.save(w);
        log.info("User {} requested withdrawal {} TZS (diamonds deducted: {})", userId, request.getAmount(), diamondsToDeduct);
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
            BigDecimal rate = getConfigDecimal(com.wakilfly.model.SystemConfig.KEY_DIAMOND_TO_TZS_RATE, com.wakilfly.model.SystemConfig.DEFAULT_DIAMOND_TO_TZS_RATE);
            BigDecimal diamondsToRefund = w.getAmount().divide(rate, 4, java.math.RoundingMode.UP);
            wallet.setDiamondBalance(wallet.getDiamondBalance().add(diamondsToRefund));
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

    private BigDecimal getConfigDecimal(String key, String defaultValue) {
        return systemConfigRepository.findByConfigKey(key)
                .map(c -> new BigDecimal(c.getConfigValue()))
                .orElse(new BigDecimal(defaultValue));
    }

    private WalletResponse mapToWalletResponse(UserWallet wallet) {
        BigDecimal rate = getConfigDecimal(com.wakilfly.model.SystemConfig.KEY_DIAMOND_TO_TZS_RATE, com.wakilfly.model.SystemConfig.DEFAULT_DIAMOND_TO_TZS_RATE);
        BigDecimal coinBuyRate = getConfigDecimal(com.wakilfly.model.SystemConfig.KEY_COIN_TO_TZS_BUY_RATE, com.wakilfly.model.SystemConfig.DEFAULT_COIN_TO_TZS_BUY_RATE);
        BigDecimal minTzs = getConfigDecimal(com.wakilfly.model.SystemConfig.KEY_WITHDRAWAL_MIN_TZS, com.wakilfly.model.SystemConfig.DEFAULT_WITHDRAWAL_MIN_TZS);
        BigDecimal maxTzs = getConfigDecimal(com.wakilfly.model.SystemConfig.KEY_WITHDRAWAL_MAX_TZS, com.wakilfly.model.SystemConfig.DEFAULT_WITHDRAWAL_MAX_TZS);
        BigDecimal diamondBalance = wallet.getDiamondBalance() != null ? wallet.getDiamondBalance() : BigDecimal.ZERO;
        return WalletResponse.builder()
                .id(wallet.getId())
                .coinBalance(wallet.getCoinBalance())
                .cashBalance(wallet.getCashBalance())
                .diamondBalance(diamondBalance)
                .totalCoinsPurchased(wallet.getTotalCoinsPurchased())
                .totalCoinsSpent(wallet.getTotalCoinsSpent())
                .totalGiftsReceived(wallet.getTotalGiftsReceived())
                .diamondToTzsRate(rate)
                .coinToTzsBuyRate(coinBuyRate)
                .withdrawableTzs(diamondBalance.multiply(rate))
                .minWithdrawalTzs(minTzs)
                .maxWithdrawalTzs(maxTzs)
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
                .level(gift.getLevel())
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
