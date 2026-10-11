package com.andynn.callwise.repository;

import com.andynn.callwise.model.Senha;
import com.andynn.callwise.model.StatusSenha;
import com.andynn.callwise.model.TipoSenha;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface SenhaRepository extends JpaRepository <Senha, Long>{
    @Query("select coalesce(max(s.numero), 0) from Senha s where s.tipo = :tipo and s.dataEmissao = :data")
    int ultimoNumeroDoDia(@Param("tipo") TipoSenha tipo, @Param("data") LocalDate data);

    boolean existsByTipoAndStatusAndDataEmissao(TipoSenha tipo, StatusSenha status, LocalDate dataEmissao);

    Optional<Senha> findFirstByTipoAndStatusAndDataEmissaoOrderByNumeroAsc(TipoSenha tipo, StatusSenha status, LocalDate dataEmissao);

    List<Senha> findTop2ByDataEmissaoAndChamadaEmIsNotNullOrderByChamadaEmDesc(LocalDate dataEmissao);
}
