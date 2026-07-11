package tech.dhjt.boot3.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.flowable.engine.repository.ProcessDefinition;

@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@Schema(description = "流程定义DTO")
public class ProcessDefinitionDTO {

    @Schema(description = "流程定义ID")
    private String id;

    @Schema(description = "流程定义Key")
    private String key;

    @Schema(description = "流程定义名称")
    private String name;

    @Schema(description = "版本号")
    private int version;

    @Schema(description = "分类")
    private String category;

    @Schema(description = "部署ID")
    private String deploymentId;

    @Schema(description = "描述")
    private String description;

    @Schema(description = "是否挂起")
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
