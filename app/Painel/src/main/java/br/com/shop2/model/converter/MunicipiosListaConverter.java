package br.com.shop2.model.converter;

import br.com.shop2.model.common.Municipios;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Converter
public class MunicipiosListaConverter implements AttributeConverter<List<Municipios>, String> {

    private static final String DELIMITADOR = "||";

    @Override
    public String convertToDatabaseColumn(List<Municipios> attribute) {
        if (attribute == null || attribute.isEmpty()) {
            return "";
        }
        return attribute.stream()
            .filter(Objects::nonNull)
            .map(Municipios::name)
            .collect(Collectors.joining(DELIMITADOR));
    }

    @Override
    public List<Municipios> convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.trim().isEmpty()) {
            return new ArrayList<>();
        }
        return Arrays.stream(dbData.split(DELIMITADOR))
            .map(String::trim)
            .filter(texto -> !texto.isEmpty())
            .map(valor -> Municipios.fromTexto(valor).orElse(null))
            .filter(Objects::nonNull)
            .collect(Collectors.toCollection(ArrayList::new));
    }
}
