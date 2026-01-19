package it.unipi.booknetapi.service.source;

import it.unipi.booknetapi.dto.source.SourceResponse;
import it.unipi.booknetapi.shared.model.Source;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class SourceService {

    Map<String, SourceResponse> sources;

    public SourceService() {
        sources = new HashMap<>();
        sources.put(
                "amazon",
                SourceResponse.builder()
                        .idSource("amazon")
                        .name("Amazon")
                        .build()
        );
        sources.put(
                "goodreads",
                SourceResponse.builder()
                        .idSource("goodreads")
                        .name("GoodReads")
                        .build()
        );
        sources.put(
                "googlebooks",
                SourceResponse.builder()
                        .idSource("googlebooks")
                        .name("Google Books")
                        .build()
        );
        sources.put(
                "kaggle",
                SourceResponse.builder()
                        .idSource("kaggle")
                        .name("Kaggle")
                        .build()
        );
    }

    public List<SourceResponse> getSources() {
        return sources.values().stream().toList();
    }

    public SourceResponse getSource(String idSource) {
        if(idSource == null) return null;

        if(!sources.containsKey(idSource)) return null;

        return sources.get(idSource);
    }

    public Source getEnumSource(String idSource) {
        if(idSource == null) return null;

        if(idSource.equalsIgnoreCase("goodreads")) return Source.GOOD_READS;
        if(idSource.equalsIgnoreCase("amazon")) return Source.AMAZON;
        if(idSource.equalsIgnoreCase("googlebooks")) return Source.GOOGLE_BOOKS;
        if(idSource.equalsIgnoreCase("kaggle")) return Source.KAGGLE;

        return null;
    }

}
