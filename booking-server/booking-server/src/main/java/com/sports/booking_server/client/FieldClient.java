package com.sports.booking_server.client;

import com.sports.booking_server.dto.FieldDTO;
import io.swagger.v3.oas.annotations.Hidden;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Hidden
@FeignClient(name = "FIELD-SERVICE")
public interface FieldClient {


    @GetMapping("/api/fields/{id}")
    FieldDTO getFieldById(@PathVariable("id") Long id);
}
