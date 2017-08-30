package service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class MapperService {

    private ObjectMapper objectMapper;

    public MapperService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    //Move to Mapper Service
    public String getJsonString(Object objectDao) {
        try {
            return objectMapper.writeValueAsString(objectDao);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public <T> Optional<T> getDao(String jsonUser, Class<T> clazz) {
        if (jsonUser == null) return Optional.empty();
        try {
            return Optional.of(objectMapper.readValue(jsonUser, clazz));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


    public <T> List<T> getDaoList(List<String> jsonList, Class<T> clazz) {
        return jsonList.stream().map(jsonString -> this.getDao(jsonString, clazz))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toList());
    }
}
