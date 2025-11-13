package br.com.shop2.domain.repository;

import br.com.shop2.model.evento.EventoExterno;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.List;

public interface EventoExternoRepository extends JpaRepository<EventoExterno, Long> {

    @Query("""
        select e from EventoExterno e
        where (:inicio is null or e.dataFim >= :inicio)
          and (:fim is null or e.dataInicio <= :fim)
        order by e.dataInicio asc, e.titulo asc
    """)
    List<EventoExterno> buscarPorPeriodo(LocalDate inicio, LocalDate fim);
}
