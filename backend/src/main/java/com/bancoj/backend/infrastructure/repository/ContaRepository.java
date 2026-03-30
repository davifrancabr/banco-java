package com.bancoj.backend.infrastructure.repository;

import com.bancoj.backend.domain.model.Conta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ContaRepository extends JpaRepository<Conta, Long> {

    Optional<Conta> findByNumero(Integer numero);

    boolean existsByNumero(Integer numero);

    @Query("SELECT COALESCE(MAX(c.numero), 10000) FROM Conta c")
    Integer findMaxNumero();

    @Query("SELECT c FROM Conta c ORDER BY c.saldo DESC")
    List<Conta> findAllOrderBySaldoDesc();

    List<Conta> findBySaldoGreaterThanEqual(Double saldo);
}
