package com.thehecklers.javaboot;

import lombok.AllArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import static org.springframework.web.reactive.function.server.RequestPredicates.GET;
import static org.springframework.web.reactive.function.server.RouterFunctions.route;
import static org.springframework.web.reactive.function.server.ServerResponse.ok;

@SpringBootApplication
public class JavaBootApplication {
    @Bean
    CommandLineRunner loadData(AirportRepository repo) {
        return args -> {
            repo.deleteAll()
                    .thenMany(
                            Flux.just(new Airport("KGAG", "Gage Airport"),
                                    new Airport("KLOL", "Derby Field"),
                                    new Airport("KBUM", "Butler Memorial Airport"),
                                    new Airport("KSTL", "St. Louis Lambert International Airport"),
                                    new Airport("KORD", "O'Hare International Airport"))
                    ).flatMap(repo::save)
                    .subscribe();
        };
    }

    @Bean
    WebClient client() {
        return WebClient.create("http://localhost:9876/metar");
    }

    @Bean
    RouterFunction<ServerResponse> routerFunction(AirportInfoService svc) {
        return route(GET("/"), svc::allAirports)
                .andRoute(GET("/{id}"), svc::airportById)
                .andRoute(GET("/metar/{id}"), svc::metar);
    }

    public static void main(String[] args) {
        SpringApplication.run(JavaBootApplication.class, args);
    }

}

@Service
@AllArgsConstructor
class AirportInfoService {
    private final AirportRepository repo;
    private final WebClient wxClient;

    public Mono<ServerResponse> allAirports(ServerRequest req) {
        return ok().body(repo.findAll(), Airport.class);
    }

    public Mono<ServerResponse> airportById(ServerRequest req) {
        return ok().body(repo.findById(req.pathVariable("id")), Airport.class);
    }

    public Mono<ServerResponse> metar(ServerRequest req) {
        return ok().body(wxClient.get()
                .uri("?loc=" + req.pathVariable("id"))
                .retrieve()
                .bodyToMono(METAR.class), METAR.class);
    }
}

interface AirportRepository extends ReactiveCrudRepository<Airport, String> {
}

@Document
record Airport(@Id String id, String name) {}

record METAR(String flight_rules, String raw) {}

/*
@Document
class Airport {
    @Id
    private String id;
    private String name;

    public Airport() {
    }

    public Airport(String id, String name) {
        this.id = id;
        this.name = name;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Airport airport = (Airport) o;
        return Objects.equals(id, airport.id) && Objects.equals(name, airport.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name);
    }

    @Override
    public String toString() {
        return "Airport{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                '}';
    }
}

class METAR {
    private String flight_rules;
    private String raw;

    public METAR() {
    }

    public METAR(String flight_rules, String raw) {
        this.flight_rules = flight_rules;
        this.raw = raw;
    }

    public String getFlight_rules() {
        return flight_rules;
    }

    public void setFlight_rules(String flight_rules) {
        this.flight_rules = flight_rules;
    }

    public String getRaw() {
        return raw;
    }

    public void setRaw(String raw) {
        this.raw = raw;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        METAR metar = (METAR) o;
        return Objects.equals(flight_rules, metar.flight_rules) && Objects.equals(raw, metar.raw);
    }

    @Override
    public int hashCode() {
        return Objects.hash(flight_rules, raw);
    }

    @Override
    public String toString() {
        return "METAR{" +
                "flight_rules='" + flight_rules + '\'' +
                ", raw='" + raw + '\'' +
                '}';
    }
}*/
