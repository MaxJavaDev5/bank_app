package ru.practicum.transfer.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.*;
import ru.practicum.transfer.dto.TransferDto;
import ru.practicum.transfer.dto.TransferResponseDto;
import ru.practicum.transfer.security.JwtUtils;
import ru.practicum.transfer.service.TransferService;

@RestController
@RequestMapping("/transfer")
@RequiredArgsConstructor
public class TransferController {

    private final TransferService transferService;

    @PostMapping
    @PreAuthorize("hasRole('USER') && hasAuthority('transfer.write')")
    public TransferResponseDto transfer(
            @Valid @RequestBody TransferDto transferDto,
            JwtAuthenticationToken authentication) {
        String fromLogin = JwtUtils.getLogin(authentication.getToken());
        return transferService.transfer(fromLogin, transferDto);
    }
}
