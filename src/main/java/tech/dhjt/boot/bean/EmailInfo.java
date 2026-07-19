package tech.dhjt.boot.bean;

import lombok.Data;
import org.hibernate.validator.constraints.UniqueElements;

import java.util.List;

/**
 * 邮箱信息实体
 * <p>
 * 根据 OrderDTO.emailType 决定哪些字段必须有值：
 * <ul>
 *   <li>1-固定邮箱：fixedEmails 必须有值</li>
 *   <li>2-业务和客户邮箱：customerEmails、businessEmails 必须有值</li>
 *   <li>3-订单邮箱：无数据要求</li>
 * </ul>
 */
@Data
public class EmailInfo {

    /** 固定邮箱列表 */
    @UniqueElements(message = "角色列表中不能包含重复的角色")
    private List<String> fixedEmails;

    /** 客户邮箱列表 */
    @UniqueElements(message = "角色列表中不能包含重复的角色")
    private List<String> customerEmails;

    /** 业务邮箱列表 */
    @UniqueElements(message = "邮箱中不能包含重复的邮箱")
    private List<String> businessEmails;

}