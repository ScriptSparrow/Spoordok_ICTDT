package nhl.stenden.spoordock;

import java.net.http.HttpClient;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import nhl.stenden.spoordock.llmService.historyManager.IChatHistoryManager;
import nhl.stenden.spoordock.llmService.historyManager.InMemoryChatHistoryManager;


@SpringBootApplication
public class SpoordockApplication {

	public static void main(String[] args) {
		SpringApplication.run(SpoordockApplication.class, args);
	}


	@Primary
	@Bean 
	public IChatHistoryManager chatHistoryManager(InMemoryChatHistoryManager inMemoryChatHistoryManager){

		//Here we can switch implementations if needed in the future
		return inMemoryChatHistoryManager;
	}

	@Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/**")
                        .allowedOrigins("*")
                        .allowedMethods("*")
                        .allowedHeaders("*");
            }
        };
    }

    @Bean
    public HttpClient httpClient() {
        return HttpClient.newHttpClient();
    }

}
