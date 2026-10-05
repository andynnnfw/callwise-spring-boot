package com.andynn.callwise.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "senha", uniqueConstraints = @UniqueConstraint(columnNames = {"tipo", "data_emissao", "numero"}))
@Getter @Setter @NoArgsConstructor
public class Senha {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    private TipoSenha tipo;
    private int numero;
    private String codigo;

    @Enumerated(EnumType.STRING)
    private StatusSenha status = StatusSenha.AGUARDANDO;
    private LocalDate dataEmissao;
    private LocalDateTime emitidaEm;
    private LocalDateTime chamadaEm;
    private LocalDateTime finalizadaEm;

}
