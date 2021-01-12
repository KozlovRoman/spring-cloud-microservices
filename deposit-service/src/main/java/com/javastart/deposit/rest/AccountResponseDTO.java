package com.javastart.deposit.rest;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class AccountResponseDTO {

    private Long accountId;
    private String name;
    private String email;
    private List<Long> bills;
    private String phone;
    private OffsetDateTime creationDate;
}
