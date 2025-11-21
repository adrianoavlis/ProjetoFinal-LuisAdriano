package br.com.shop2.domain.repository;

import br.com.shop2.model.common.EvolucaoFiltro;
import br.com.shop2.model.dados.GastoMensal;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;

import java.time.YearMonth;
import java.util.Collection;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class EvolucaoRepository {

    private final GastoMensalRepository gastoMensalRepository;

    public List<GastoMensal> buscarPorFiltro(EvolucaoFiltro filtro) {
        Specification<GastoMensal> spec = null;

        YearMonth mesInicio = filtro != null ? filtro.getMesInicio() : null;
        YearMonth mesFim = filtro != null ? filtro.getMesFim() : null;
        Integer anoReferencia = filtro != null ? filtro.getAnoReferencia() : null;
        boolean possuiLimiteDeMes = mesInicio != null || mesFim != null;

        if (mesInicio != null) {
            Specification<GastoMensal> condicao = (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("mesAno"), mesInicio);
            spec = spec == null ? condicao : spec.and(condicao);
        }
        if (mesFim != null) {
            Specification<GastoMensal> condicao = (root, query, cb) -> cb.lessThanOrEqualTo(root.get("mesAno"), mesFim);
            spec = spec == null ? condicao : spec.and(condicao);
        }
        if (anoReferencia != null && !possuiLimiteDeMes) {
            Specification<GastoMensal> condicao = (root, query, cb) ->
                cb.equal(cb.function("year", Integer.class, root.get("mesAno")), anoReferencia);
            spec = spec == null ? condicao : spec.and(condicao);
        }

        if (spec == null) {
            return gastoMensalRepository.findAll();
        }
        return gastoMensalRepository.findAll(spec);
    }

    public List<GastoMensal> buscarMenorPreco(Collection<String> municipios,
                                              boolean municipiosVazio,
                                              YearMonth mesInicio,
                                              YearMonth mesFim,
                                              Integer anoReferencia) {
        return gastoMensalRepository.buscarMenorPreco(municipios, municipiosVazio, mesInicio, mesFim, anoReferencia);
    }

    public List<GastoMensal> listarTodos() {
        return gastoMensalRepository.findAll();
    }

    public List<YearMonth> listarPeriodosImportados() {
        return gastoMensalRepository.listarPeriodosImportados();
    }
}
