package tech.dhjt.boot.convert;

import org.mapstruct.*;
import org.mapstruct.factory.Mappers;
import tech.dhjt.boot.bean.Order;
import tech.dhjt.boot.bean.dto.OrderDTO;
import tech.dhjt.boot.enums.OrderStatusEnum;

@Mapper(componentModel = "spring", imports = { OrderStatusEnum.class })
public interface OrderConvert {

    // 如果需要非 Spring 环境可用静态方法获取实例，保留这个：
    OrderConvert INSTANCE = Mappers.getMapper(OrderConvert.class);

    @Mapping(target = "id", ignore = true)
//    @Mapping(target = "status", expression = "java(OrderStatusEnum.PENDING)")
    @Mapping(target = "status", constant = "PENDING") // ✅ 当源值为 null 时给默认值
//    @Mapping(target = "status", source = "status", defaultValue = "PENDING")
    Order toBeanForAdd(OrderDTO dto);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "status", ignore = true)
    void toBeanForUpdate(OrderDTO dto, @MappingTarget Order order);

    OrderDTO toDTO(Order order);

//    // 字段名不同时，使用 @Mapping 指定
//    @Mapping(source = "id", target = "userId")
//    @Mapping(source = "name", target = "userName")
//    @Mapping(source = "email", target = "userEmail")
//    // 自定义转换方法：birthDate -> age
//    @Mapping(source = "birthDate", target = "age", qualifiedByName = "birthDateToAge")
//    @Mapping(source = "active", target = "activeStatus")
//    UserDTO toDTO(User user);
//
//    // 反向转换同样需要定义
//    @Mapping(source = "userId", target = "id")
//    @Mapping(source = "userName", target = "name")
//    @Mapping(source = "userEmail", target = "email")
//    @Mapping(target = "birthDate", ignore = true)   // age 不能逆推出生日，忽略或自定义
//    @Mapping(source = "activeStatus", target = "active")
//    User toEntity(UserDTO dto);
//
//    // 自定义年龄计算
//    @Named("birthDateToAge")
//    static int birthDateToAge(LocalDate birthDate) {
//        return birthDate != null ? Period.between(birthDate, LocalDate.now()).getYears() : 0;
//    }

}
