package com.wakilfly.service;

import com.wakilfly.dto.response.*;
import com.wakilfly.exception.BadRequestException;
import com.wakilfly.exception.ResourceNotFoundException;
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
     * Get user's wallet
     */
    public WalletResponse getWallet(UUID userId) {
        UserWallet wallet = getOrCreateWallet(userId);
        return mapToWalletResponse(wallet);
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
