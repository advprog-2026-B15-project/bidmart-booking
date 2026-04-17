package com.example.bidmartbooking.booking.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class NotificationPreferenceResponse {
    private String userId;
    private Boolean emailEnabled;
    private Boolean inAppEnabled;
}
