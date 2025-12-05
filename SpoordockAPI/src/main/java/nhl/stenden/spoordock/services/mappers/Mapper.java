package nhl.stenden.spoordock.services.mappers;

import java.util.List;

public interface Mapper <DTO, ENTITY> {

    DTO toDTO(ENTITY entity);
    ENTITY toEntity(DTO dto);

    List<DTO> toDTOs(List<ENTITY> entities);
}
