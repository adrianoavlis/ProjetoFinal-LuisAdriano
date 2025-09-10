package br.com.shop2.controller;

import br.com.shop2.api.dto.CestaBasicaPreviewDTO;
import br.com.shop2.domain.service.CestaBasicaService;
import br.com.shop2.model.mercado.CestaBasica;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.IntStream;

@Controller
@RequiredArgsConstructor
@RequestMapping("/cesta")
public class CestaBasicaController {

    private final CestaBasicaService service;

    /* ===================== PÁGINA JSP ===================== */

    /** GET: tela com formulário e (opcionalmente) a tabela já carregada */
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
        return "cesta/importar"; // /WEB-INF/views/cesta/importar.jsp
    }

    /** POST: Buscar dados (preview, sem gravar) */
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

    /** POST: Confirmar & Salvar — recebe apenas os índices selecionados */
    @PostMapping("/salvar")
    public String salvarSelecionados(
            @RequestParam("mesIni") String mesIni,
            @RequestParam("mesFim") String mesFim,
            @RequestParam("estado[]") List<String> estados,
            @RequestParam("mesAno[]") List<String> meses,
            @RequestParam("valor[]") List<Double> valores,
            @RequestParam(value = "sel[]", required = false) List<Integer> selecionados,
            Model model
    ) {
        List<CestaBasicaPreviewDTO> preview = new ArrayList<>();
        for (int i = 0; i < estados.size(); i++) {
            preview.add(CestaBasicaPreviewDTO.builder()
                    .estado(estados.get(i))
                    .mesAno(meses.get(i))
                    .valor(valores.get(i))
                    .build());
        }

        List<CestaBasicaPreviewDTO> toSave = new ArrayList<>();
        if (selecionados != null) {
            for (Integer idx : selecionados) {
                if (idx >= 0 && idx < preview.size()) {
                    toSave.add(preview.get(idx));
                }
            }
        }
        int inseridos = service.salvarSelecao(toSave);

        model.addAttribute("mesIni", mesIni);
        model.addAttribute("mesFim", mesFim);
        model.addAttribute("preview", preview); // mantém na tela após salvar
        model.addAttribute("salvo", Map.of("inseridos", inseridos, "selecionados", (toSave != null ? toSave.size() : 0)));
        return "cesta/importar";
    }

    /* ======= Compatibilidade: importação local e o /scrape antigo ======= */
    @PostMapping("/importarLocal")
    @ResponseBody
    public Map<String,Integer> importarLocal(@RequestParam String path) {
        return service.importarTabelaLocal(path);
    }

    @GetMapping("/scrape")
    @ResponseBody
    public String scrape(@RequestParam String filePath) {
        service.scrapeAndSave(filePath);
        return "Scraping concluído e dados salvos no banco!";
    }

    /* ===================== REST CRUD SIMPLES ===================== */
    @GetMapping("/api")
    @ResponseBody
    public List<CestaBasica> listar() {
        return service.listarTudo();
    }

    @GetMapping("/api/by")
    @ResponseBody
    public List<CestaBasica> listarPorUfMes(
            @RequestParam String uf,
            @RequestParam String mesAno
    ) {
        return service.listarPorUfMes(uf, mesAno);
    }

    @PostMapping("/api")
    @ResponseBody
    public CestaBasica criar(@RequestBody CestaBasica cb) {
        return service.salvar(cb);
    }

    @DeleteMapping("/api/{id}")
    @ResponseBody
    public ResponseEntity<Void> excluir(@PathVariable Long id) {
        service.excluir(id);
        return ResponseEntity.noContent().build();
    }
}
