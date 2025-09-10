package br.com.shop2.domain.repository;

import br.com.shop2.model.mercado.CestaBasica;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CestaBasicaRepository extends JpaRepository<CestaBasica, Long> {

    List<CestaBasica> findByEstadoAndMesAnoOrderByIdAsc(String estado, String mesAno);

    boolean existsByEstadoAndMesAnoAndValor(String estado, String mesAno, Double valor);
}
