package com.beyond.order_system.member.controller;

import com.beyond.order_system.common.auth.JwtTokenProvider;
import com.beyond.order_system.member.domain.Member;
import com.beyond.order_system.member.dtos.*;
import com.beyond.order_system.member.service.MemberService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/member")
public class MemberController {
    private final MemberService memberService;
    private final JwtTokenProvider jwtTokenProvider;
    @Autowired
    public MemberController(MemberService memberService, JwtTokenProvider jwtTokenProvider) {
        this.memberService = memberService;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @PostMapping("/create")
    @Operation(
            summary = "회원가입", description = "이메일, 비밀번호를 통한 회원가입"
    )
    public ResponseEntity<?> create(@RequestBody @Valid MemberCreateReqDto dto){
        Long id = memberService.create(dto);
        return ResponseEntity.status(HttpStatus.OK).body(id);
    }

    @PostMapping("/doLogin")
    public ResponseEntity<?> doLogin(@RequestBody MemberLoginReqDto dto){
        Member member = memberService.login(dto);
//        AT, RT 생성 및 저장
        MemberLoginResDto memberLoginResDto = MemberLoginResDto.builder()
                .accessToken(jwtTokenProvider.createToken(member))
                .refreshToken(jwtTokenProvider.createRtToken(member))
                .build();
        return ResponseEntity.status(HttpStatus.OK).body(memberLoginResDto);
    }

    @Transactional(readOnly = true)
    @GetMapping("/list")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> findAll(){
        List<MemberListResDto> memberListResDto = new ArrayList<>();
        memberListResDto = memberService.findAll();
        return ResponseEntity.status(HttpStatus.OK).body(memberListResDto);
    }

    @GetMapping("/myinfo")
    public ResponseEntity<?> findMyInfo(@AuthenticationPrincipal String email){
        MemberDetailResDto memberDetailResDto = memberService.findMyInfo(email);
        return ResponseEntity.status(HttpStatus.OK).body(memberDetailResDto);
    }

    @GetMapping("/detail/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> findById(@PathVariable("id") Long id){
        MemberDetailResDto memberDetailResDto = memberService.findById(id);
        return ResponseEntity.status(HttpStatus.OK).body(memberDetailResDto);
    }

    @PostMapping("/refresh-at")
    public ResponseEntity<?> refreshAt(@RequestBody RefreshTokenDto dto){
//        rt 겁증(1. 토큰 자체 검증 2. redis 조회 검증)
        Member member = jwtTokenProvider.validateRt(dto.getRefreshToken());

//        at 신규 생성
        MemberLoginResDto memberLoginResDto = MemberLoginResDto.builder()
                .accessToken(jwtTokenProvider.createToken(member))
                .refreshToken(null)
                .build();
        return ResponseEntity.status(HttpStatus.OK).body(memberLoginResDto);
    }


}
