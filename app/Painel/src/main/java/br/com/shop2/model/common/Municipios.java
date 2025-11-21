package br.com.shop2.model.common;

import java.text.Normalizer;
import java.util.Arrays;
import java.util.Collection;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;

/** Municípios cobertos pelo painel da cesta básica. */
public enum Municipios {
    ARACAJU("Aracaju"),
    BELEM("Belém"),
    BELO_HORIZONTE("Belo Horizonte"),
    BOA_VISTA("Boa Vista"),
    BRASILIA("Brasília"),
    CAMPO_GRANDE("Campo Grande"),
    CUIABA("Cuiabá"),
    CURITIBA("Curitiba"),
    FLORIANOPOLIS("Florianópolis"),
    FORTALEZA("Fortaleza"),
    GOIANIA("Goiânia"),
    JOAO_PESSOA("João Pessoa"),
    MACAE("Macaé"),
    MACAPA("Macapá"),
    MACEIO("Maceió"),
    MANAUS("Manaus"),
    NATAL("Natal"),
    PALMAS("Palmas"),
    PORTO_ALEGRE("Porto Alegre"),
    PORTO_VELHO("Porto Velho"),
    RECIFE("Recife"),
    RIO_BRANCO("Rio Branco"),
    RIO_DE_JANEIRO("Rio de Janeiro"),
    SALVADOR("Salvador"),
    SAO_LUIS("São Luís"),
    SAO_PAULO("São Paulo"),
    TERESINA("Teresina"),
    VITORIA("Vitória");

    private final String nome;
    private final String normalizado;

    Municipios(String nome) {
        this.nome = nome;
        this.normalizado = normalizar(nome);
    }

    public String getNome() {
        return nome;
    }

    public String getId() {
        return name();
    }

    public String getNormalizado() {
        return normalizado;
    }

    public static Optional<Municipios> fromTexto(String texto) {
        if (texto == null) {
            return Optional.empty();
        }
        String normalizado = normalizar(texto);
        if (normalizado.isEmpty()) {
            return Optional.empty();
        }
        return Arrays.stream(values())
            .filter(item -> item.normalizado.equals(normalizado) || item.name().equalsIgnoreCase(texto.trim()))
            .findFirst();
    }

    public static String normalizar(String texto) {
        if (texto == null) {
            return "";
        }
        String trimmed = texto.trim();
        if (trimmed.isEmpty()) {
            return "";
        }
        String semAcento = Normalizer.normalize(trimmed, Normalizer.Form.NFD)
            .replaceAll("\\p{M}+", "");
        return semAcento.toUpperCase(Locale.ROOT);
    }

    public static String concatenarNomes(Collection<Municipios> municipios, String delimitador) {
        if (municipios == null || municipios.isEmpty()) {
            return "";
        }
        return municipios.stream()
            .map(Municipios::getNome)
            .collect(Collectors.joining(delimitador));
    }

    public static String concatenarIds(Collection<Municipios> municipios, String delimitador) {
        if (municipios == null || municipios.isEmpty()) {
            return "";
        }
        return municipios.stream()
            .map(Municipios::name)
            .collect(Collectors.joining(delimitador));
    }

    public static <C extends Collection<Municipios>> C filtrarReconhecidos(Collection<String> nomes, java.util.function.Supplier<C> fornecedorColecao) {
        C resultado = fornecedorColecao.get();
        if (nomes == null) {
            return resultado;
        }
        for (String nome : nomes) {
            fromTexto(nome).ifPresent(resultado::add);
        }
        return resultado;
    }
}
