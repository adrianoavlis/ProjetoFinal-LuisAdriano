package br.com.shop2.domain.repository;

import br.com.shop2.model.mercado.CestaBasica;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.List;

@Repository
public interface CestaBasicaRepository extends JpaRepository<CestaBasica, Long> {

    List<CestaBasica> findByMunicipioAndMesAnoOrderByIdAsc(String municipio, String mesAno);

    boolean existsByMunicipioAndMesAno(String municipio, String mesAno);

    Optional<CestaBasica> findByMunicipioAndMesAno(String municipio, String mesAno);

}
