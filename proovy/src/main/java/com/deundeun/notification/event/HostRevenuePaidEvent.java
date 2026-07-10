package com.deundeun.notification.event;

public record HostRevenuePaidEvent(
    Long userId,
    Long hostRevenueId
) {
}
