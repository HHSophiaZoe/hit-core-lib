import com.hit.spring.SpringStarterConfig;
import com.hit.spring.core.exception.StreamingException;
import com.hit.spring.core.reactive.DataStream;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@SpringBootTest(classes = SpringStarterConfig.class)
public class DataStreamTest {

    private static final List<String> MOCK_DATA = List.of("A", "B", "C", "D", "F", "G", "H", "J", "K");

    @Test
    public void testDataStream() {
        List<String> actualData = new ArrayList<>();
        DataStream<List<String>> streaming = this.streamingSource();
        streaming.subscribe(
                batch -> {
                    for (String data : batch) {
                        log.info("[testDataStream] Batch: {}", data);
                        actualData.add(data);
                    }
                },
                error -> {
                    log.error("[testDataStream] Error while processing data: {}", error.getMessage(), error);
                },
                () -> {
                    log.info("[testDataStream] Success");
                }
        );
        log.info("[testDataStream] ActualData: {}", actualData);
        Assertions.assertEquals(MOCK_DATA, actualData);
    }

    public DataStream<List<String>> streamingSource() {
        return DataStream.create(emitter -> {
            for (String data : MOCK_DATA) {
                try {
                    List<String> nextData = new ArrayList<>();
                    nextData.add(data);
                    emitter.onNext(nextData);
                } catch (Exception e) {
                    emitter.onError(new StreamingException(e.getMessage(), e));
                }
            }
            emitter.onComplete();
        });
    }
}
