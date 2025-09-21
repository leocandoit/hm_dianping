package com.hmdp.mapper;

import com.hmdp.entity.User;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

/**
 * <p>
 *  Mapper 接口
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
public interface UserMapper extends BaseMapper<User> {
    //继承了BaseMapper之后，已经有了i
    // insert(User user)
    // deleteById(…)
    // updateById(…)
    // selectById(…)

    // 相当于DAO
    // @Select("select * from tb_user where phone = #{phone} limit 1")
    // User queryByPhone(String phone);
}
