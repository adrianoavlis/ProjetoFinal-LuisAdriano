package br.com.shop2.controller;

import br.com.shop2.domain.service.CestaBasicaService;
import br.com.shop2.model.common.CestaBasicaPreviewDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@Controller
@RequiredArgsConstructor
@RequestMapping("/cesta")
public class CestaBasicaController {

    private final CestaBasicaService service;

    /** Página importar.jsp (form + tabela) */
    @GetMapping("/ui")
    public String ui(
            @RequestParam(value = "mesIni", required = false) String mesIni,
            @RequestParam(value = "mesFim", required = false) String mesFim,
            Model model
    ) {
        model.addAttribute("mesIni", mesIni == null ? "" : mesIni);
        model.addAttribute("mesFim", mesFim == null ? "" : mesFim);
        model.addAttribute("preview", List.of());
        model.addAttribute("salvo", null);
        return "cesta/importar";
    }

    /** Botão "Buscar": chama Selenium e traz preview (sem gravar) */
    @PostMapping("/buscar")
    public String buscar(
            @RequestParam("mesAnoInicial") String mesIni,
            @RequestParam("mesAnoFinal") String mesFim,
            Model model
    ) {
        List<CestaBasicaPreviewDTO> preview = service.previewDieese(mesIni, mesFim);
        model.addAttribute("mesIni", mesIni);
        model.addAttribute("mesFim", mesFim);
        model.addAttribute("preview", preview);
        model.addAttribute("salvo", null);
        return "cesta/importar";
    }

    /** Botão "Confirmar": envia apenas os itens selecionados */
    @PostMapping("/salvar")
    public String salvarSelecionados(
            @RequestParam("mesIni") String mesIni,
            @RequestParam("mesFim") String mesFim,
            @RequestParam("municipio[]") List<String> municipio,
            @RequestParam("mesAno[]") List<String> mesAno,
            @RequestParam("valor[]") List<Double> valores,
            @RequestParam(value = "sel[]", required = false) List<Integer> selecionados,
            Model model
    ) {
        List<CestaBasicaPreviewDTO> preview = new ArrayList<>();
        for (int i = 0; i < municipio.size(); i++) {
            preview.add(CestaBasicaPreviewDTO.builder()
                .municipio(municipio.get(i))
                .mesAno(mesAno.get(i))
                .valor(valores.get(i))
                .build());
        }

        List<CestaBasicaPreviewDTO> toSave = new ArrayList<>();
        if (selecionados != null) {
            for (Integer idx : selecionados) {
                if (idx >= 0 && idx < preview.size()) toSave.add(preview.get(idx));
            }
        }
        int inseridos = service.salvarSelecao(toSave);

        model.addAttribute("mesIni", mesIni);
        model.addAttribute("mesFim", mesFim);
        model.addAttribute("preview", preview); // mantém a lista na tela
        model.addAttribute("salvo", Map.of("processados", toSave.size(), "mergeados", inseridos));
        return "cesta/importar";
    }
}
