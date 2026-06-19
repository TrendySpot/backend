package com.spotz.domain.admin;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DashboardResponse {
    private long totalMembers;
    private long totalSpots;
    private long totalTickets;
    private long totalReviews;
    private long totalRevenue;
}
