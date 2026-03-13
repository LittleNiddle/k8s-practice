package com.beyond.order_system.member.dtos;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@AllArgsConstructor
@NotBlank
@Data
@Builder
public class MemberLoginResDto {
    private String accessToken;
    private String refreshToken;
}
