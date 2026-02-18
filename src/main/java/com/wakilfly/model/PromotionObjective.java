package com.wakilfly.model;

/** Campaign objective – defines what the algorithm optimizes for (Meta-style). */
public enum PromotionObjective {
    AWARENESS,   // Reach maximum people
    TRAFFIC,     // Drive to website/app
    ENGAGEMENT,  // Likes, comments, shares
    MESSAGES,    // DM/conversations
    LEADS,       // Collect lead info
    CONVERSIONS  // Sales, signups
}
