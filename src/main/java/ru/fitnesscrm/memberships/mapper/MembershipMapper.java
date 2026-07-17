package ru.fitnesscrm.memberships.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import ru.fitnesscrm.memberships.api.dto.response.ClientMembershipResponse;
import ru.fitnesscrm.memberships.api.dto.response.MembershipTemplateResponse;
import ru.fitnesscrm.memberships.domain.ClientMembership;
import ru.fitnesscrm.memberships.domain.MembershipTemplate;

import java.util.List;

@Mapper(componentModel = "spring")
public interface MembershipMapper {

    @Mapping(target = "templateId", source = "template.id")
    @Mapping(target = "templateName", source = "template.name")
    ClientMembershipResponse toClientMembershipResponse(ClientMembership membership);

    List<ClientMembershipResponse> toClientMembershipResponseList(List<ClientMembership> memberships);

    MembershipTemplateResponse toMembershipTemplateResponse(MembershipTemplate template);

    List<MembershipTemplateResponse> toMembershipTemplateResponseList(List<MembershipTemplate> templates);
}
