package com.example.mapper;

import com.example.entity.YcUserAccount;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface YcUserAccountMapper {

    @Select("SELECT * FROM yc_user_account WHERE username = #{username} LIMIT 1")
    YcUserAccount selectByUsername(@Param("username") String username);

    @Select("SELECT * FROM yc_user_account WHERE id = #{id} LIMIT 1")
    YcUserAccount selectById(@Param("id") Long id);

    @Insert("INSERT INTO yc_user_account(username, password_hash, display_name, role, status) " +
            "VALUES(#{username}, #{passwordHash}, #{displayName}, #{role}, #{status})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(YcUserAccount user);
}
