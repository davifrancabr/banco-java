package com.bancoj.backend.domain.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "contas")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "tipo", discriminatorType = DiscriminatorType.STRING)
@Data
@NoArgsConstructor
public abstract class Conta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private Integer numero;

    @Column(nullable = false)
    private String titular;

    @Column(nullable = false)
    private Double saldo;

    @OneToMany(mappedBy = "conta", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("dataHora ASC")
    private List<HistoricoOperacao> historico = new ArrayList<>();

    public Conta(Integer numero, String titular, Double saldo) {
        this.numero = numero;
        this.titular = titular;
        this.saldo = saldo;
    }

    public void depositar(double saldo) {
        this.saldo += saldo;
    }

    public boolean sacar(double valor) {
        if (valor > this.saldo) return false;
        this.saldo -= valor;
        return true;
    }

    public abstract double calcularRendimento();
    public abstract double calcularTributo();
    public abstract String getTipoDescricao();
}
