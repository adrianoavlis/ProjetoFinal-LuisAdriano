package br.com.shop2.model.converter;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Converter
public class MunicipiosListaConverter implements AttributeConverter<List<String>, String> {

    private static final String DELIMITADOR = "||";

    @Override
    public String convertToDatabaseColumn(List<String> attribute) {
        if (attribute == null || attribute.isEmpty()) {
            return "";
        }
        return attribute.stream()
            .filter(Objects::nonNull)
            .map(String::trim)
            .filter(texto -> !texto.isEmpty())
            .collect(Collectors.joining(DELIMITADOR));
    }

    @Override
    public List<String> convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.trim().isEmpty()) {
            return new ArrayList<>();
        }
        return Arrays.stream(dbData.split(DELIMITADOR))
            .map(String::trim)
            .filter(texto -> !texto.isEmpty())
            .collect(Collectors.toCollection(ArrayList::new));
    }
}
