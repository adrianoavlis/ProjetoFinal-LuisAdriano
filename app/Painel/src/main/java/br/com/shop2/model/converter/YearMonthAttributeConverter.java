package br.com.shop2.model.converter;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.sql.Date;
import java.time.YearMonth;

@Converter(autoApply = true)
public class YearMonthAttributeConverter implements AttributeConverter<YearMonth, Date> {

    @Override
    public Date convertToDatabaseColumn(YearMonth attribute) {
        if (attribute == null) {
            return null;
        }
        return Date.valueOf(attribute.atDay(1));
    }

    @Override
    public YearMonth convertToEntityAttribute(Date dbData) {
        if (dbData == null) {
            return null;
        }
        return YearMonth.from(dbData.toLocalDate());
    }
}
