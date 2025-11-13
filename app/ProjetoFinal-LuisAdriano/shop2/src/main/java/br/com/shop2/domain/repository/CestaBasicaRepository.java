package br.com.shop2.domain.repository;

import br.com.shop2.model.mercado.CestaBasica;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CestaBasicaRepository extends JpaRepository<CestaBasica, Long> {

    boolean existsByMunicipioAndMesAnoAndValor(String municipio, String mesAno, Double valor);

    List<CestaBasica> findByMunicipioAndMesAnoOrderByIdAsc(String municipio, String mesAno);

    Optional<CestaBasica> findByMunicipioAndMesAno(String municipio, String mesAno);

    @Query("""
        select cb.mesAno as mesAno, avg(cb.valor) as mediaValor
        from CestaBasica cb
        group by cb.mesAno
    """)
    List<SerieHistoricaProjection> calcularMediaMensal();

    @Query("""
        select distinct upper(cb.municipio)
        from CestaBasica cb
        where cb.municipio is not null and trim(cb.municipio) <> ''
        order by upper(cb.municipio)
    """)
    List<String> listarMunicipiosOrdenados();

    @Query("""
        select upper(cb.municipio) as municipio, cb.mesAno as mesAno, avg(cb.valor) as mediaValor
        from CestaBasica cb
        group by upper(cb.municipio), cb.mesAno
    """)
    List<SerieMunicipioProjection> calcularMediaMensalPorMunicipio();

    @Query("""
        select upper(cb.municipio) as municipio, cb.mesAno as mesAno, avg(cb.valor) as mediaValor
        from CestaBasica cb
        where upper(cb.municipio) in :municipios
        group by upper(cb.municipio), cb.mesAno
    """)
    List<SerieMunicipioProjection> calcularMediaMensalPorMunicipio(@Param("municipios") List<String> municipios);

    interface SerieHistoricaProjection {
        String getMesAno();
        Double getMediaValor();
    }

    interface SerieMunicipioProjection {
        String getMunicipio();
        String getMesAno();
        Double getMediaValor();
    }

}
