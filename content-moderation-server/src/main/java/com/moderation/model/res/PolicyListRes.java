package com.moderation.model.res;

import com.moderation.skillos.model.PolicyDefinition;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class PolicyListRes {
    private List<PolicyDefinition> policies;
}
