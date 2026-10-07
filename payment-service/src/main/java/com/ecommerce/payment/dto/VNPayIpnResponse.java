package com.ecommerce.payment.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VNPayIpnResponse {

    @JsonProperty("RspCode")
    private String rspCode; // "00" = Thành công, "97" = Chữ ký không hợp lệ, "01" = Order not found, "02" = Order already confirmed

    @JsonProperty("Message")
    private String message;
}
