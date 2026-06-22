package ru.practicum.accounts.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import ru.practicum.accounts.dto.AccountDto;
import ru.practicum.accounts.dto.AccountShortDto;
import ru.practicum.accounts.dto.UpdateAccountDto;
import ru.practicum.accounts.model.Account;

import java.util.List;

@Mapper(componentModel = "spring")
public interface AccountMapper {

    AccountDto toAccountDto(Account account);

    AccountShortDto toAccountShortDto(Account account);

    List<AccountShortDto> toAccountShortDtoList(List<Account> accounts);

    void updateAccountFromDto(UpdateAccountDto updateDto, @MappingTarget Account account);
}
