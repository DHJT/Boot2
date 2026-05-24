package tech.dhjt.boot3.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.flowable.engine.repository.ProcessDefinition;

@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class ProcessDefinitionDTO {
    private String id;
    private String key;
    private String name;
    private int version;
    private String category;
    private String deploymentId;
    private String description;
    private boolean suspended;

    // 转换方法
    public static ProcessDefinitionDTO convertToDTO(ProcessDefinition pd) {
        ProcessDefinitionDTO dto = new ProcessDefinitionDTO();
        dto.setId(pd.getId());
        dto.setKey(pd.getKey());
        dto.setName(pd.getName());
        dto.setVersion(pd.getVersion());
        dto.setCategory(pd.getCategory());
        dto.setDeploymentId(pd.getDeploymentId());
        dto.setDescription(pd.getDescription());
        dto.setSuspended(pd.isSuspended());
        return dto;
    }
}
