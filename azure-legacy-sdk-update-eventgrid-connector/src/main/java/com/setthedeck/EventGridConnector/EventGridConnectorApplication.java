package com.setthedeck.EventGridConnector;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class EventGridConnectorApplication implements CommandLineRunner {

	public static void main(String[] args) {
		SpringApplication.run(EventGridConnectorApplication.class, args);
	}

	@Override
	public void run(String... args) throws Exception {
		Connect connect = new Connect();
		connect.Push();
	}

}
