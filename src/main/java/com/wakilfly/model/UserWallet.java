package com.wakilfly.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "user_wallets")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserWallet {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "coin_balance")
    @Builder.Default
    private Integer coinBalance = 0; // Coins for buying gifts

    @Column(name = "cash_balance", precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal cashBalance = BigDecimal.ZERO; // Legacy; use diamondBalance for creator earnings

    /** Creator earnings from gifts (diamonds). Convert to TZS at rate for withdrawal. */
    @Column(name = "diamond_balance", precision = 15, scale = 4)
    @Builder.Default
    private BigDecimal diamondBalance = BigDecimal.ZERO;

    @Column(name = "total_coins_purchased")
    @Builder.Default
    private Integer totalCoinsPurchased = 0;

    @Column(name = "total_coins_spent")
    @Builder.Default
    private Integer totalCoinsSpent = 0;

    @Column(name = "total_gifts_received", precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal totalGiftsReceived = BigDecimal.ZERO;

    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Helper methods
    public void addCoins(int coins) {
        this.coinBalance += coins;
        this.totalCoinsPurchased += coins;
    }

    public boolean spendCoins(int coins) {
        if (this.coinBalance >= coins) {
            this.coinBalance -= coins;
            this.totalCoinsSpent += coins;
            return true;
        }
        return false;
    }

    public void addCash(BigDecimal amount) {
        this.cashBalance = this.cashBalance.add(amount);
        this.totalGiftsReceived = this.totalGiftsReceived.add(amount);
    }

    public void addDiamonds(BigDecimal diamonds) {
        if (diamonds == null) return;
        this.diamondBalance = (this.diamondBalance != null ? this.diamondBalance : BigDecimal.ZERO).add(diamonds);
    }
}
