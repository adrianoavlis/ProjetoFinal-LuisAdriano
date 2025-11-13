package br.com.shop2.model.dados;

import br.com.shop2.model.common.BaseEntity;
import br.com.shop2.model.converter.YearMonthAttributeConverter;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Entity
@Table(name = "gastos_mensais",
    uniqueConstraints = @UniqueConstraint(name = "uk_gasto_municipio_mes", columnNames = {"municipio", "mes_ano"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GastoMensal extends BaseEntity {

    private static final DateTimeFormatter OUT_FORMATTER = DateTimeFormatter.ofPattern("MM/yyyy");

    @Column(nullable = false, length = 150)
    private String municipio;

    @Convert(converter = YearMonthAttributeConverter.class)
    @Column(name = "mes_ano", nullable = false)
    private YearMonth mesAno;

    @Column(name = "total_cesta", precision = 10, scale = 2, nullable = false)
    private BigDecimal totalCesta;

    @Column(precision = 10, scale = 2)
    private BigDecimal carne;

    @Column(precision = 10, scale = 2)
    private BigDecimal leite;

    @Column(precision = 10, scale = 2)
    private BigDecimal feijao;

    @Column(precision = 10, scale = 2)
    private BigDecimal arroz;

    @Column(precision = 10, scale = 2)
    private BigDecimal farinha;

    @Column(precision = 10, scale = 2)
    private BigDecimal batata;

    @Column(precision = 10, scale = 2)
    private BigDecimal tomate;

    @Column(name = "pao", precision = 10, scale = 2)
    private BigDecimal pao;

    @Column(precision = 10, scale = 2)
    private BigDecimal cafe;

    @Column(precision = 10, scale = 2)
    private BigDecimal banana;

    @Column(name = "acucar", precision = 10, scale = 2)
    private BigDecimal acucar;

    @Column(name = "oleo", precision = 10, scale = 2)
    private BigDecimal oleo;

    @Column(precision = 10, scale = 2)
    private BigDecimal manteiga;

    private static final Pattern UF_PARENTESIS = Pattern.compile("(.+?)\\(([^)\\s]{2})\\)\\s*$");
    private static final Pattern UF_HIFEN = Pattern.compile("(.+?)[-–—]\\s*([A-Za-z]{2})\\s*$");

    @Transient
    public String getMesAnoFormatado() {
        return mesAno == null ? "" : OUT_FORMATTER.format(mesAno);
    }

    @Transient
    public String getUf() {
        if (municipio == null) {
            return "";
        }

        String texto = municipio.trim();
        if (texto.isEmpty()) {
            return "";
        }

        String[] barraSplit = texto.split("/");
        if (barraSplit.length == 2) {
            String candidato = barraSplit[1].trim();
            if (isUfValida(candidato)) {
                return candidato.toUpperCase(Locale.ROOT);
            }
        }

        Matcher parentese = UF_PARENTESIS.matcher(texto);
        if (parentese.find()) {
            String candidato = parentese.group(2);
            if (isUfValida(candidato)) {
                return candidato.toUpperCase(Locale.ROOT);
            }
        }

        Matcher hifen = UF_HIFEN.matcher(texto);
        if (hifen.find()) {
            String candidato = hifen.group(2);
            if (isUfValida(candidato)) {
                return candidato.toUpperCase(Locale.ROOT);
            }
        }

        String[] partes = texto.split("\\s+");
        if (partes.length > 1) {
            String candidato = partes[partes.length - 1].trim();
            if (isUfValida(candidato)) {
                return candidato.toUpperCase(Locale.ROOT);
            }
        }

        return "";
    }

    private boolean isUfValida(String candidato) {
        return candidato != null
            && candidato.length() == 2
            && candidato.chars().allMatch(Character::isLetter);
    }
}
