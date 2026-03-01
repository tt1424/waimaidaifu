package com.daifu.manage;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@MapperScan("com.daifu.manage.**.mapper")
@SpringBootApplication
public class DaifuManageApplication {
   //测试
    public static void main(String[] args) {
        SpringApplication.run(DaifuManageApplication.class, args);
    }
}
