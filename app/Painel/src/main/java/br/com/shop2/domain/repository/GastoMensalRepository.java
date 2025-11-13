package br.com.shop2.domain.repository;

import br.com.shop2.model.dados.GastoMensal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.time.YearMonth;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface GastoMensalRepository extends JpaRepository<GastoMensal, Long>, JpaSpecificationExecutor<GastoMensal> {

    Optional<GastoMensal> findByMunicipioIgnoreCaseAndMesAno(String municipio, YearMonth mesAno);

    @Query("select distinct g.mesAno from GastoMensal g order by g.mesAno desc")
    List<YearMonth> listarPeriodosImportados();

    @Query("""
        select g from GastoMensal g
        where (:municipiosVazio = true or upper(g.municipio) in :municipios)
          and (:mesInicio is null or g.mesAno >= :mesInicio)
          and (:mesFim is null or g.mesAno <= :mesFim)
          and (:anoReferencia is null or function('year', g.mesAno) = :anoReferencia)
          and g.totalCesta = (
            select min(g2.totalCesta) from GastoMensal g2
            where (:municipiosVazio = true or upper(g2.municipio) in :municipios)
              and (:mesInicio is null or g2.mesAno >= :mesInicio)
              and (:mesFim is null or g2.mesAno <= :mesFim)
              and (:anoReferencia is null or function('year', g2.mesAno) = :anoReferencia)
          )
        order by g.totalCesta asc, g.mesAno asc, g.municipio asc
    """)
    List<GastoMensal> buscarMenorPreco(Collection<String> municipios,
                                       boolean municipiosVazio,
                                       YearMonth mesInicio,
                                       YearMonth mesFim,
                                       Integer anoReferencia);
}
