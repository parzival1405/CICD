package com.example.wallet;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
// Thêm code Bước 1 - mục 6 "Thực hành từng bước", Bài 10 Giai đoạn 1: tự tìm các lớp @ConfigurationProperties
@ConfigurationPropertiesScan
public class WalletServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(WalletServiceApplication.class, args);
	}

}
