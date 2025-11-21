package br.com.shop2.controller;

import br.com.shop2.domain.service.EventoExternoService;
import br.com.shop2.domain.service.GastoMensalService;
import br.com.shop2.domain.service.GastoMensalService.ImportacaoResultado;
import br.com.shop2.model.common.Municipios;
import br.com.shop2.model.dados.GastoMensal;
import br.com.shop2.model.dados.ImportacaoResumoView;
import br.com.shop2.model.dados.PeriodoImportacaoView;
import br.com.shop2.model.evento.EventoExternoForm;
import br.com.shop2.model.evento.Impacto;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.validation.Valid;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.validation.BindingResult;

@Controller
@RequestMapping("/dados")
@RequiredArgsConstructor
public class DadosController {

    private static final DateTimeFormatter FORMATO_EXIBICAO = DateTimeFormatter.ofPattern("MM/yyyy", Locale.of("pt", "BR"));
    private static final YearMonth PERIODO_INICIAL = YearMonth.of(2000, 1);
    private static final YearMonth PERIODO_FINAL = YearMonth.of(2025, 12);

    private final GastoMensalService gastoMensalService;
    private final EventoExternoService eventoExternoService;

    @GetMapping
    public String telaDados(@RequestParam(value = "municipio", required = false) String municipio,
                            @RequestParam(value = "periodo", required = false) String periodo,
                            @RequestParam(value = "page", defaultValue = "0") int page,
                            @RequestParam(value = "size", defaultValue = "20") int size,
                            Model model) {

        int pagina = Math.max(page, 0);
        List<Integer> tamanhosPermitidos = List.of(10, 20, 50, 100);
        int tamanho = tamanhosPermitidos.contains(size) ? size : 20;

        YearMonth filtroPeriodo = gastoMensalService.parseEntrada(periodo);
        Pageable pageable = PageRequest.of(pagina, tamanho, Sort.by(Sort.Order.desc("mesAno"), Sort.Order.asc("municipio")));
        Page<GastoMensal> dados = gastoMensalService.listar(municipio, filtroPeriodo, pageable);

        List<YearMonth> periodosImportados = gastoMensalService.listarPeriodosImportados();

        if (!model.containsAttribute("eventoForm")) {
            model.addAttribute("eventoForm", EventoExternoForm.builder()
                .impacto(Impacto.NEGATIVO)
                .build());
        }

        model.addAttribute("dados", dados);
        model.addAttribute("municipioFiltro", municipio);
        model.addAttribute("periodoFiltro", periodo);
        model.addAttribute("size", tamanho);
        model.addAttribute("tamanhosPermitidos", tamanhosPermitidos);
        model.addAttribute("periodosImportados", formatarPeriodos(periodosImportados));
        model.addAttribute("linhaPeriodos", montarLinhaPeriodos(periodosImportados));
        model.addAttribute("eventosExternos", eventoExternoService.listarTodos());
        model.addAttribute("municipiosEventos", Municipios.values());

        return "dados";
    }

    @PostMapping("/importar")
    public String importar(@RequestParam("dataInicial") String dataInicial,
                           @RequestParam("dataFinal") String dataFinal,
                           @RequestParam(value = "sequencial", defaultValue = "false") boolean sequencial,
                           RedirectAttributes redirectAttributes) {
        try {
            ImportacaoResultado resultado = gastoMensalService.importar(dataInicial, dataFinal, sequencial);
            ImportacaoResumoView view = converter(resultado);
            redirectAttributes.addFlashAttribute("resultadoImportacao", view);
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("erroImportacao", ex.getMessage());
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("erroImportacao", "Falha ao importar: " + ex.getMessage());
        }
        return "redirect:/dados";
    }

    @PostMapping("/eventos")
    public String criarEvento(@Valid EventoExternoForm form,
                              BindingResult bindingResult,
                              RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("eventoErro", extrairMensagensErro(bindingResult));
            redirectAttributes.addFlashAttribute("eventoForm", form);
            return "redirect:/dados#eventos-externos";
        }
        try {
            eventoExternoService.criar(form);
            redirectAttributes.addFlashAttribute("eventoSucesso", "Evento cadastrado com sucesso.");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("eventoErro", ex.getMessage());
            redirectAttributes.addFlashAttribute("eventoForm", form);
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("eventoErro", "Falha ao salvar evento: " + ex.getMessage());
            redirectAttributes.addFlashAttribute("eventoForm", form);
        }
        return "redirect:/dados#eventos-externos";
    }

    @PostMapping("/eventos/{id}/atualizar")
    public String atualizarEvento(@PathVariable("id") Long id,
                                  @Valid EventoExternoForm form,
                                  BindingResult bindingResult,
                                  RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("eventoErro", extrairMensagensErro(bindingResult));
            redirectAttributes.addFlashAttribute("eventoForm", form);
            return "redirect:/dados#eventos-externos";
        }
        try {
            eventoExternoService.atualizar(id, form);
            redirectAttributes.addFlashAttribute("eventoSucesso", "Evento atualizado com sucesso.");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("eventoErro", ex.getMessage());
            redirectAttributes.addFlashAttribute("eventoForm", form);
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("eventoErro", "Falha ao atualizar evento: " + ex.getMessage());
            redirectAttributes.addFlashAttribute("eventoForm", form);
        }
        return "redirect:/dados#eventos-externos";
    }

    @PostMapping("/eventos/{id}/excluir")
    public String excluirEvento(@PathVariable("id") Long id,
                                RedirectAttributes redirectAttributes) {
        try {
            eventoExternoService.excluir(id);
            redirectAttributes.addFlashAttribute("eventoSucesso", "Evento removido com sucesso.");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("eventoErro", "Falha ao remover evento: " + ex.getMessage());
        }
        return "redirect:/dados#eventos-externos";
    }

    private ImportacaoResumoView converter(ImportacaoResultado resultado) {
        List<String> periodosSucesso = Optional.ofNullable(resultado.getPeriodosSucesso()).orElse(List.of()).stream()
            .map(this::formatar)
            .collect(Collectors.toList());
        Map<String, String> erros = Optional.ofNullable(resultado.getErrosPorPeriodo()).orElse(Map.of()).entrySet().stream()
            .collect(Collectors.toMap(entry -> formatar(entry.getKey()), Map.Entry::getValue));
        return ImportacaoResumoView.builder()
            .totalProcessados(resultado.getTotalProcessados())
            .totalInseridos(resultado.getTotalInseridos())
            .totalAtualizados(resultado.getTotalAtualizados())
            .periodosSucesso(periodosSucesso)
            .errosPorPeriodo(erros)
            .build();
    }

    private List<PeriodoImportacaoView> montarLinhaPeriodos(List<YearMonth> periodosImportados) {
        Set<YearMonth> importados = new HashSet<>(Optional.ofNullable(periodosImportados).orElse(List.of()));
        List<PeriodoImportacaoView> linha = new ArrayList<>();

        YearMonth cursor = PERIODO_INICIAL;
        while (!cursor.isAfter(PERIODO_FINAL)) {
            linha.add(new PeriodoImportacaoView(formatar(cursor), importados.contains(cursor)));
            cursor = cursor.plusMonths(1);
        }

        return linha;
    }

    private List<String> formatarPeriodos(List<YearMonth> periodos) {
        return Optional.ofNullable(periodos).orElse(List.of()).stream()
            .map(this::formatar)
            .collect(Collectors.toList());
    }

    private String formatar(YearMonth mes) {
        if (mes == null) {
            return "";
        }
        return FORMATO_EXIBICAO.format(mes);
    }

    private String extrairMensagensErro(BindingResult bindingResult) {
        return bindingResult.getAllErrors().stream()
            .map(DefaultMessageSourceResolvable::getDefaultMessage)
            .filter(Objects::nonNull)
            .collect(Collectors.joining(". "));
    }
}
