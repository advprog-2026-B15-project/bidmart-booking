package com.example.bidmartbooking.booking.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateNotificationPreferenceRequest {
    private Boolean emailEnabled;
    private Boolean inAppEnabled;
}
