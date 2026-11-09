package cn.xny.tvservice;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@MapperScan("cn.xny.tvservice.mapper")
public class TvserviceApplication {

    public static void main(String[] args) {
        SpringApplication.run(TvserviceApplication.class, args);
    }
}
