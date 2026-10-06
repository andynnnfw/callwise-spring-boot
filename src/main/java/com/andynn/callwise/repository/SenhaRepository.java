package com.andynn.callwise.repository;

import com.andynn.callwise.model.Senha;
import com.andynn.callwise.model.TipoSenha;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;

public interface SenhaRepository extends JpaRepository <Senha, Long>{
    @Query("select coalesce(max(s.numero), 0) from Senha s where s.tipo = :tipo and s.dataEmissao = :data")
    int ultimoNumeroDoDia(@Param("tipo") TipoSenha tipo, @Param("data") LocalDate data);
}
