package com.beyond.order_system.member.dtos;

import com.beyond.order_system.member.domain.Member;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class MemberCreateReqDto {
    @NotBlank
    private String name;
    @NotBlank
    private String email;
    @NotBlank
    private String password;

    public Member toEntity(String password){
        return Member.builder()
                .name(this.name)
                .email(this.email)
                .password(password)
                .build();
    }
}
