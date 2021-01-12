package com.javastart.deposit.repository;

import com.javastart.deposit.entity.Deposit;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface DepositRepository extends CrudRepository<Deposit, Long> {

    List<Deposit> findDepositsByEmail(String email);

}
