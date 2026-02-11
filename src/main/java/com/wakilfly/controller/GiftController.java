package com.wakilfly.controller;

import com.wakilfly.dto.request.UserWithdrawalRequest;
import com.wakilfly.dto.response.*;
import com.wakilfly.security.CustomUserDetailsService;
import com.wakilfly.model.CoinPackage;
import com.wakilfly.model.PaymentType;
import com.wakilfly.repository.CoinPackageRepository;
import com.wakilfly.service.GiftService;
import com.wakilfly.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class GiftController {

    private final GiftService giftService;
    private final PaymentService paymentService;
    private final CustomUserDetailsService userDetailsService;
    private final CoinPackageRepository coinPackageRepository;

    // ============================================
    // COIN PACKAGES
    // ============================================

    /**
     * Get available coin packages
     * GET /api/v1/coins/packages
     */
    @GetMapping("/coins/packages")
    public ResponseEntity<ApiResponse<List<CoinPackageResponse>>> getCoinPackages() {
        List<CoinPackageResponse> packages = giftService.getCoinPackages();
        return ResponseEntity.ok(ApiResponse.success(packages));
    }

    /**
     * Initiate coin purchase (returns payment URL/transaction)
     * POST /api/v1/coins/purchase
     */
    @PostMapping("/coins/purchase")
    public ResponseEntity<ApiResponse<Map<String, String>>> initiatePurchase(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody Map<String, String> request) {
        UUID userId = userDetailsService.loadUserEntityByUsername(userDetails.getUsername()).getId();
        UUID packageId = UUID.fromString(request.get("packageId"));
        String phone = request.get("phone");

        CoinPackage pkg = coinPackageRepository.findById(packageId)
                .orElseThrow(() -> new com.wakilfly.exception.ResourceNotFoundException("Package not found"));

        String transactionId = paymentService.initiatePayment(
                userId,
                pkg.getPrice(),
                PaymentType.COIN_PURCHASE,
                phone,
                "Purchase " + pkg.getName(),
                packageId);

        return ResponseEntity.ok(ApiResponse.success("Payment initiated", Map.of("transactionId", transactionId)));
    }

    // ============================================
    // VIRTUAL GIFTS
    // ============================================

    /**
     * Get available gifts
     * GET /api/v1/gifts
     */
    @GetMapping("/gifts")
    public ResponseEntity<ApiResponse<List<VirtualGiftResponse>>> getGifts() {
        List<VirtualGiftResponse> gifts = giftService.getAvailableGifts();
        return ResponseEntity.ok(ApiResponse.success(gifts));
    }

    /**
     * Get gifts sent during a live (viewers see total coins / gifts; host stores & converts later)
     * GET /api/v1/gifts/live/{liveStreamId}
     */
    @GetMapping("/gifts/live/{liveStreamId}")
    public ResponseEntity<ApiResponse<PagedResponse<GiftTransactionResponse>>> getGiftsForLive(
            @PathVariable UUID liveStreamId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "30") int size) {
        PagedResponse<GiftTransactionResponse> gifts = giftService.getGiftsForLive(liveStreamId, page, size);
        return ResponseEntity.ok(ApiResponse.success(gifts));
    }

    /**
     * Send a gift
     * POST /api/v1/gifts/send
     */
    @PostMapping("/gifts/send")
    public ResponseEntity<ApiResponse<Void>> sendGift(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody Map<String, Object> request) {
        UUID senderId = userDetailsService.loadUserEntityByUsername(userDetails.getUsername()).getId();
        UUID receiverId = UUID.fromString((String) request.get("receiverId"));
        UUID giftId = UUID.fromString((String) request.get("giftId"));
        int quantity = request.get("quantity") != null ? (Integer) request.get("quantity") : 1;
        String message = (String) request.get("message");
        UUID liveStreamId = request.get("liveStreamId") != null
                ? UUID.fromString((String) request.get("liveStreamId"))
                : null;

        giftService.sendGift(senderId, receiverId, giftId, liveStreamId, quantity, message);
        return ResponseEntity.ok(ApiResponse.success("Gift sent successfully"));
    }

    // ============================================
    // WALLET
    // ============================================

    /**
     * Get my wallet (coin balance + cash balance from gifts – host anaweza kuconvert cash to pesa)
     * GET /api/v1/wallet
     */
    @GetMapping("/wallet")
    public ResponseEntity<ApiResponse<WalletResponse>> getWallet(
            @AuthenticationPrincipal UserDetails userDetails) {
        UUID userId = userDetailsService.loadUserEntityByUsername(userDetails.getUsername()).getId();
        WalletResponse wallet = giftService.getWallet(userId);
        return ResponseEntity.ok(ApiResponse.success(wallet));
    }

    /**
     * Request withdrawal of gift cash to pesa (convert balance → mobile money)
     * POST /api/v1/wallet/withdraw
     */
    @PostMapping("/wallet/withdraw")
    public ResponseEntity<ApiResponse<UserCashWithdrawalResponse>> requestWithdrawal(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody UserWithdrawalRequest request) {
        UUID userId = userDetailsService.loadUserEntityByUsername(userDetails.getUsername()).getId();
        UserCashWithdrawalResponse withdrawal = giftService.requestCashWithdrawal(userId, request);
        return ResponseEntity.ok(ApiResponse.success("Withdrawal request submitted", withdrawal));
    }

    /**
     * Get my withdrawal history (gift cash → pesa)
     * GET /api/v1/wallet/withdrawals
     */
    @GetMapping("/wallet/withdrawals")
    public ResponseEntity<ApiResponse<PagedResponse<UserCashWithdrawalResponse>>> getMyWithdrawals(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        UUID userId = userDetailsService.loadUserEntityByUsername(userDetails.getUsername()).getId();
        PagedResponse<UserCashWithdrawalResponse> withdrawals = giftService.getMyWithdrawals(userId, page, size);
        return ResponseEntity.ok(ApiResponse.success(withdrawals));
    }

    /**
     * Get received gifts history
     * GET /api/v1/wallet/gifts/received
     */
    @GetMapping("/wallet/gifts/received")
    public ResponseEntity<ApiResponse<PagedResponse<GiftTransactionResponse>>> getReceivedGifts(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        UUID userId = userDetailsService.loadUserEntityByUsername(userDetails.getUsername()).getId();
        PagedResponse<GiftTransactionResponse> gifts = giftService.getReceivedGifts(userId, page, size);
        return ResponseEntity.ok(ApiResponse.success(gifts));
    }

    /**
     * Get sent gifts history
     * GET /api/v1/wallet/gifts/sent
     */
    @GetMapping("/wallet/gifts/sent")
    public ResponseEntity<ApiResponse<PagedResponse<GiftTransactionResponse>>> getSentGifts(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        UUID userId = userDetailsService.loadUserEntityByUsername(userDetails.getUsername()).getId();
        PagedResponse<GiftTransactionResponse> gifts = giftService.getSentGifts(userId, page, size);
        return ResponseEntity.ok(ApiResponse.success(gifts));
    }
}
