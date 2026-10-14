package ru.net.avz.test.moex_alert.common.dto;

import jakarta.validation.constraints.NotNull;
import ru.net.avz.test.moex_alert.common.AbstractEntity;
import ru.net.avz.test.moex_alert.common.fields.EntityIdField;

public abstract class AbstractDto {

    @NotNull
    @EntityIdField
    public final java.util.UUID id;

    public AbstractDto(AbstractEntity entity) {
        this.id = entity.getId();
    }
}
